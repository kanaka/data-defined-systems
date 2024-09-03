const express = require('express')
const Knex = require('knex')
const { Model } = require('objection')
const knexConfig = require('./knexfile')

const knex = Knex(knexConfig.development)
Model.knex(knex)

const app = express()
app.use(express.json())

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

function error(res, status, msg) {
  return res.status(status).json({ error: msg })
}

// Actions
app.get('/users', async (req, res) => {
  console.info(`>>> querying users`)
  const users = await User.query()
  console.info(`<<< queried  users: ${JSON.stringify(users)}`)
  res.json(users)
})

app.post('/users', async (req, res) => {
  console.info(`>>> creating user: '${JSON.stringify(req.body)}'`)
  const newUser = await User.query().insert(req.body)
  console.info(`<<< created  user: '${JSON.stringify(newUser)}'`)
  res.status(201).json(newUser)
})

app.get('/users/:id', async (req, res) => {
  console.info(`>>> querying /user/${id}`)
  const user = await User.query().findById(req.params.id)
  if (user) {
    console.info(`<<< queried  /user/${id}: '${JSON.stringify(user)}'`)
    res.json(user)
  } else {
    return error(res, 404, 'User not found')
  }
})

app.put('/users/:id', async (req, res) => {
  if (req.headers['content-type'] !== 'application/json') {
    return error(res, 415, 'Content-Type must be application/json')
  }
  const { id } = req.params
  const { version, ...updateData } = req.body

  if (version === undefined) {
    return error(res, 409, 'version missing')
  }
  try {
    console.info(`>>> updating /user/${id}: '${JSON.stringify(req.body)}'`)
    const updatedUser = await User.query()
      .findById(id)
      .where('version', version) // Ensure the version matches
      .patch({
        ...updateData,
        version: version + 1 // Increment the version
      })
      .returning('*')

    if (updatedUser) {
      console.log(`<<< updated  /user/${id}: '${JSON.stringify(updatedUser)}`)
      res.json(updatedUser)
    } else {
      return error(res, 409, 'version/atomicity violation')
    }
  } catch (error) {
    return error(res, 500, 'Internal server error')
  }
})

app.delete('/users/:id', async (req, res) => {
  console.info(`Deleting /user/${id}`)
  await User.query().deleteById(req.params.id)
  res.status(204).end()
})

// Start the server
app.listen(8000, () => {
  console.log('Server is running on port 8000')
})

