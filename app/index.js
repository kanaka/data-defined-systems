const os = require('os')
const express = require('express')
const Knex = require('knex')
const { Model } = require('objection')
const knexConfig = require('./knexfile')

const knex = Knex(knexConfig.development)
Model.knex(knex)

const app = express()
app.use(express.json())

let serverId, buggy = false

// Model
class User extends Model {
  static get tableName() {
    return 'users'
  }

  static get jsonSchema() {
    return {
      type: 'object',
      required: ['name', 'email'],
      properties: {
        id: { type: 'integer' },
        name: { type: 'string', minLength: 1, maxLength: 255 },
        email: { type: 'string', format: 'email' },
        version: { type: 'integer' }
      }
    }
  }
}

function delay(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

function error(res, status, msg) {
  console.warn(`<<< ${status} - error: ${msg}`)
  return res.status(status).json({ error: msg })
}

// Actions
app.get('/users', async (req, res) => {
  console.info(`>>> GET - querying users`)
  const users = await User.query()
  console.info(`<<< 200 - queried  users: ${JSON.stringify(users)}`)
  res.json(users)
})

app.post('/users', async (req, res) => {
  console.info(`>>> POST - creating user: '${JSON.stringify(req.body)}'`)
  if (req.headers['content-type'] !== 'application/json') {
    return error(res, 415, 'Content-Type must be application/json')
  }
  try {
    const newUser = await User.query().insert(req.body)
    res.json({"message": `"Created user ${newUser.id}"`}).status(201)
    console.info(`<<< 201 - created  user: '${JSON.stringify(newUser)}'`)
  } catch (exception) {
    return error(res, 500, 'Internal server error')
  }
})

app.get('/users/:id', async (req, res) => {
  const { id } = req.params
  console.info(`>>> GET - querying /user/${id}`)
  const user = await User.query().findById(id)
  if (user) {
    res.json(user)
    console.info(`<<< 200 - queried  /user/${id}: '${JSON.stringify(user)}'`)
  } else {
    return error(res, 404, 'User not found')
  }
})

app.put('/users/:id', async (req, res) => {
  const { id } = req.params
  let updateData = req.body
  console.info(`>>> PUT - updating /user/${id}: '${JSON.stringify(updateData)}'`)
  if (req.headers['content-type'] !== 'application/json') {
    return error(res, 415, 'Content-Type must be application/json')
  }

  try {
    const user = await User.query().findById(id)
    if (user) {
      const curVersion = user.version
      await user.$query().patch(updateData).returning('*')
      res.json({"message": `"Updated user ${id}"`}).status(203).end()

      if (buggy) {
        console.warn(`    BUG: doing extra work before version update to /user/${id}`)
        await delay(200)
      }
      const newVersion = curVersion + 1
      updatedUser = await User.query().findById(id).patch({version: newVersion}).returning('*')
      console.log(`<<< 203 - updated  /user/${id}: '${JSON.stringify(updatedUser)}`)
    } else {
      return error(res, 404, 'User not found')
    }
  } catch (exception) {
    return error(res, 500, `Internal server error: ${exception}`)
  }
})

app.delete('/users/:id', async (req, res) => {
  const { id } = req.params
  console.info(`>>> DELETE - deleting /user/${id}`)
  await User.query().deleteById(id)
  res.json({}).status(204).end()
  console.info(`<<< 204 - deleted /user/${id}`)
})

app.put('/reset', async (req, res) => {
  console.info(`>>> PUT - reseting/deleting all users`)
  try {
    await User.query().delete()
    await knex.raw('ALTER SEQUENCE users_id_seq RESTART WITH 1');
    res.json({'message':'All users deleted'}).end()
    console.info(`<<< 200 - reset/deleted all users`)
  } catch (exception) {
    console.error(exception)
    return error(res, 500, 'Error deleting users')
  }
})

async function main() {
  const ifname = process.argv[2]
  console.log(`Waiting for ${ifname} to be configured`)
  while (!serverId) {
    const intf = os.networkInterfaces()[ifname]?.find(iface => iface.family === 'IPv4' && !iface.internal)
    serverId = intf?.address.split('.').pop()
    if (!serverId) await new Promise(resolve => setTimeout(resolve, 100))
  }

  if (process.env["BUGGY"] && {"2":1,"101":1}[serverId]) {
    console.warn(`Buggy PUT triggered for serverId ${serverId}`)
    buggy = true
  }

  // Start the server
  app.listen(8000, () => {
    console.log(`Server ${serverId} is running on port 8000`)
  })
}

main()
