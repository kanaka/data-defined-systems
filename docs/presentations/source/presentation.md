### Tools that Enable Data-Defined and Containerized Testing of Multi-Service Networked Systems

Setup Page

<br>
<br>
<br>
<span>
<button class="start-share-screen" data-targets="video0 video1 video2 video3 video4 video5">Start terminal share</button>
<button class="stop-share-screen" data-targets="video0 video1 video2 video3 video4 video5">Stop terminal share</button>
<br>
<video id="video0" style="width:30%; height:30%;" autoplay muted></video>
</span>
<br>
<!--
<span>
<button class="start-share-screen" data-targets="web0 web1">Start web share</button>
<button class="stop-share-screen" data-targets="web0 web1">Stop web share</button>
<br>
<video id="web0" style="width:30%; height:30%;" autoplay muted></video>
</span>
-->

Notes:

* Setup:
  * start split presenter/presentation windows
  * Start two terminals
    * Large font size in one (different desktop) 80x44 x 2
    * Small font size (same desktop as presenter window)
  * Set in large terminal:
    * `PROMPT_COMMAND='echo -ne "\033]0;Demo\007"'`
  * Share large terminal
  * Open grafana in audience browser window (make sure logged in)
  * tmix setup:
    * windows: bash, direct, static, dhcp, monitor
    * export `COMPOSE_PROJECT_NAME` in each
    * make sure right dc is on path
    * clear all frames

-----

### Tools that Enable Data-Defined and Containerized Testing of Multi-Service Networked Systems

<br>
<br>

Clojure/conj 2024

*Joel Martin*

<br>
<figure class="fragment">
  <img src="chatgpt-more-yaml.webp" width=25%>
  <figcaption>Image: What this system needs is more YAML!<br>(ChatGPT prompt)</i></figcaption>
</figure>

Notes:
 
* Reset timer
* Welcome everyone to first session of the morning
* [Read title]
* Or as this talk could have been titled:
  "What this system needs is more YAML!"

---

### About Me

* Clojure engineer at LonoCloud in 2011
* LonoCloud was acquired by Viasat in 2013
* Started at Equinix this year (2024)
* PhD in CS in 2019 (Generative Testing of Browser Render Engines)
* Clojurescript-in-Clojurescript (2013 Clojure West)
* Created Make-a-Lisp / mal (Clojure West 2014 lightning talk)
* Open source: noVNC, websockify, raft.js, miniMAL, wac, wam

Notes:

- start with a quick personal intro
* Today I will cover some other open source projects I am involved
  with

-----

### Intro

_Tools that Enable Data-Defined and Containerized Testing of Multi-Service Networked Systems_

- Tools heavy and demo focused
- Themes:
  - data-defined
  - containerization
  - clojure


Notes:

- A bit of a mouthful

- Focus of this presentation is on tools so it will be demo heavy.
- However, there are themes woven through these tools that I want to
  emphasize:
    - data-defined tools development and testing.
    - containerization
    - clojure: the major tools I'm going to show are written in Clojure
- lot to cover so I won't take question during the talk but I would
  love to answer any questions people have either in the discussion
  channel or in the hall-way track!

---

### What is a data-defined approach?

<br>
<br>

**Data-defined systems put data in control, driving both the logic and
behavior.**

Notes:

- Both "data-driven" and "data-defined" are already overloaded
  term or art in some contexts. Other terms in this space that also
  aren't quite right are "declarative", "spec-driven", and "data-first".
- "Data-defined" is probably better than what's currently in the
  abstract. So I'm going to use that going forward.

---

### Why data-defined?

- Faster dev and test loop
- Greater interoperability and openness
- Easier testing and behavior simulation
- Separation of concerns
- Explicit vs implicit

<br>
<br>

<div>

Lisp syntax is data-defined!

Clojure:
```clojure
(defmacro unless
  [p a b]
  `(if ~p ~b ~a))
```

</div>
<!-- .element class="fragment" data-fragment-index="1" -->

<div>

miniMAL:
```json
["def", "unless", ["~",
  ["fn", ["p","a","b"],
    ["list", ["`", "if"], "p", "b", "a"]]]]
```

</div>
<!-- .element class="fragment" data-fragment-index="2" -->

Notes:


- Faster dev and test loop: less time in compile/build
- Greater interoperability and openness: other tools can consume and generate data
- Easier testing and behavior simulation: especially generative testing
- Separation of concerns: logic and behavior are more distinct
- Explicit vs implicit: logic is less hidden within code


- Given the name on this conference I'm obliged to point out that
  Clojure itself emobodies a data-defined approach:
    - The miniMAL project I mentioned takes this to an extreme: the
      code is in fact raw JSON.

---

### Data-defined protocols

#### [clj-protocol](https://github.com/lonocloud/clj-protocol)

<div class="columns">
  <div class="column column-5">

```clojure
(def MSG-TYPE-LIST
  [;; num, message,         resp,   broadcast
   [1      :DISCOVER        :OFFER   true]
   [2      :OFFER           nil      nil]
   [3      :REQUEST         :ACK     true]
   [4      :DECLINE         nil      nil]
   [5      :ACK             nil      nil]
   [6      :NAK             nil      nil]
   [7      :RELEASE         :ACK     false]
   [8      :INFORM          :ACK     false]
   ...
   ])

(def MSG-TYPE-LOOKUP (fields/list->lookup MSG-TYPE-LIST [0 1] [1 0]))

;; https://datatracker.ietf.org/doc/html/rfc2132
(def OPTS-LIST
  ;; code,  name,              type      extra-context
  [[53  :opt/msg-type          :lookup   {:lookup-type :uint8
                                          :lookup MSG-TYPE-LOOKUP}]
   [1   :opt/netmask           :ipv4     nil]
   [3   :opt/router            :repeat   {:repeat-type :ipv4 :repeat-size 4}]
   [4   :opt/time-servers      :repeat   {:repeat-type :ipv4 :repeat-size 4}]
   [5   :opt/name-servers      :repeat   {:repeat-type :ipv4 :repeat-size 4}]
   [6   :opt/dns-servers       :repeat   {:repeat-type :ipv4 :repeat-size 4}]
   [12  :opt/hostname          :utf8     nil]
   [15  :opt/domainname        :utf8     nil]
   [28  :opt/mtu               :uint16   nil]
   [28  :opt/broadcast         :ipv4     nil]
   [41  :opt/nis-servers       :repeat   {:repeat-type :ipv4 :repeat-size 4}]
   [43  :opt/vend-spec-info    :raw      nil]
   [50  :opt/addr-req          :ipv4     nil]
   [51  :opt/lease-time        :uint32   nil]
   [54  :opt/dhcp-server-id    :ipv4     nil]
   ...
   ])

(def OPTS-LOOKUP (tlvs/tlv-list->lookup OPTS-LIST))


```
<!-- .element class="reduce-font-more" -->

  </div>
  <div class="column column-5">

```clojure
;; https://datatracker.ietf.org/doc/html/rfc2131
(def DHCP-FLAGS [[:broadcast  :bool   1]
                 [:reserved   :int   15]])

(def DHCP-HEADER
;;  name,          type,      extra-context
  [[:op            :uint8     {:default 0}]
   [:htype         :uint8     {:default 1}]
   [:hlen          :uint8     {:default 6}]
   [:hops          :uint8     {:default 0}]
   [:xid           :uint32    {:default 0}]
   [:secs          :uint16    {:default 0}]
   [:flags         :bitfield  {:length 2 :default 0 :spec DHCP-FLAGS}]
   [:ciaddr        :ipv4      {:default "0.0.0.0"}]
   [:yiaddr        :ipv4      {:default "0.0.0.0"}]
   [:siaddr        :ipv4      {:default "0.0.0.0"}] ;; next server
   [:giaddr        :ipv4      {:default "0.0.0.0"}]
   [:chaddr        :mac       {:default "00:00:00:00:00:00"}]
   [:chaddr-extra  :raw       {:length 10 :default [0 0 0 0 0 0 0 0 0 0]}]
   [:sname         :utf8      {:length 64 :default ""}]
   [:bootfile      :utf8      {:length 128 :default ""}] ;; :file
   [:cookie        :raw       {:length 4 :default [99 130 83 99]}]
   [:options       :tlv-map   {:tlv-tsize 1
                               :tlv-lsize 1
                               :lookup OPTS-LOOKUP}]])

(def HEADERS-FIXED {:htype  1
                    :hlen   6
                    :hops   0 ;; fixed until relay supported
                    :cookie [99 130 83 99]}) ;; 0x63825363
```
<!-- .element class="reduce-font-more" -->


  </div>
</div>

Notes:

- another data-defined example
- clj-protocol takes a data-defined approach to network protocols
- clj-protocol includes a definition of the DHCP protocol
- sort of looks like it was pulled straight out of standards spec
  document ... well, that's because it essentially was!


---

### DHCP server implementation


<div class="columns">
  <div class="column column-5">

Postgres IP pool assignment:

```clojure
(defn pg-select-all
  [client table]
  (P/let [result (.query client (str "SELECT * FROM " table ";"))]
    (js->clj (.-rows result) :keywordize-keys true)))

(defn pg-insert-row
  [client table row]
  (P/let [ks (S/join ", " (map name (keys row)))
          vnums (S/join ", " (map #(str "$" %1) (range 1 (inc (count row)))))
          sql (str "INSERT INTO " table " (" ks ")" " VALUES (" vnums ")")
          result (.query client sql (clj->js (vals row)))]
    result))

(defn query-or-assign-ip
  [{:keys [pg-opts pg-table dhcp-cfg]} mac]
  (P/let
    [pg-client (doto (pg.Client. (clj->js pg-opts))
                 .connect)
     rows (pg-select-all pg-client pg-table)
     reassign-ip (:ip (first (filter #(= (:mac %) mac) rows)))
     ip (or reassign-ip
            (P/let [used-set (set (map :ip rows))
                    all-ips (addrs/ip-seq (:start dhcp-cfg) (:end dhcp-cfg))
                    assign-ip (some #(if (contains? used-set %) nil %)
                                    all-ips)
                    res (pg-insert-row pg-client pg-table {:mac mac
                                                           :ip assign-ip})]
              assign-ip))]
    (.end pg-client)
    (when ip
      (merge dhcp-cfg {:ip ip
                       :action (if reassign-ip "Reassigning" "Assigning")}))))

```
<!-- .element class="reduce-font-more" -->

  </div>
  <div class="column column-5">

DHCP message handler and NATS events:

```clojure
(defn nats-publish
  [client subject data]
  (P/let [sc (nats/StringCodec)
          msg (.encode sc (js/JSON.stringify (clj->js data)))]
    (.publish client subject msg)))

(defn pool-handler
  "Takes a parsed DHCP client message `msg-map`, queries the DB
  for assigned IPs or assigns one, sends a NAT event, and then
  responds to the client with the assigned address."
  [{:keys [log-msg server-info nats-cfg nats-client] :as cfg} msg-map]
  (P/let [field-overrides (:fields cfg) ;; config file field/option overrides
          mac (:chaddr msg-map)
          dhcp-cfg (query-or-assign-ip cfg mac)]
    (if (not dhcp-cfg)
      (log-msg :error (str "MAC " mac " could not be queried"))
      (P/let [{:keys [action ip gateway netmask]} dhcp-cfg]
        (when nats-client
          (let [{:keys [server subject target-port]} nats-cfg
                msg {:action "add"
                     :target (str ip ":" target-port)}]
            (log-msg :info (str "Publishing to '" server "': " msg))
            (nats-publish nats-client subject msg)))
        (log-msg :info (str action " " ip "/" netmask " to " mac
                            (when gateway " (gateway " gateway ")")))
        (merge
          (dhcp/default-response msg-map server-info)
          (select-keys msg-map [:giaddr :opt/relay-agent-info])
          {:yiaddr ip
           :opt/netmask netmask}
          (when gateway {:opt/router [gateway]})
          field-overrides)))))
```
<!-- .element class="reduce-font-more" -->


  </div>
</div>

Notes:

- a data-defined approach combined with Clojure gives really flexible
  and powerful way to quickly create service prototypes (or testing
  mocks, etc) for arbitrary binary network protocols
    - this shows most of the code needed to create a full DHCP server
      using clj-protocol
        - on left is the code for using postgres as IP pool assignment
        - on right is the DHCP message handler and function that sends
          DHCP events to NATS.

---

### Why not data-defined?

- Can be less readable
- Tendency for data sprawl
- Harder to trace and debug

Notes:

- not all roses, there are some potential downsides to data-defined
  approach
- Can be less readable: behavior can be emergent rather hard-coded in code
- Data sprawl: can be harder to determine interactions and
  dependencies between different parts of the data
- Harder to trace and debug: downside of logic and behavior being distinct
    - e.g. a stack trace may point a line of code that is opaque
      without the additional data context

- for the most part, these downsides aren't fundamental but rather
  point to a tooling gap

-----

### The System
<!-- .slide: class="fullslide" data-transition="none" -->

<img src="demo-composition-full.png" style="width: 60%"/>

Notes:
- here is the full system that I will be using to demonstrate the
  tools and data-defined philosophy
- quick summary:
  - DB store
  - Horizontally scale application or API nodes
  - load balancer in front of them
  - app nodes get IP via DHCP
    - this is the data-defined DHCP server shown earlier
  - DHCP server sends api scale events to message bus
  - balancer listens to events and updates target config
  - monitoring on right
  - testing below that
- has many elements of what make a production system difficult to
  understand, develop, and test
    - use this system as a model to show how a data-defined approach
      can help with these problems
    - even this model is too much to start with ...

---

### The System: simplified
<!-- .slide: class="fullslide" data-transition="none" -->

<img src="demo-composition-simple.png" style="width: 60%"/>

Notes:

- this is reduced to the functional essentials:
  - data storage and application logic
- how do we configure and instantiate this system in a data-defined
  way?

-----

### Data-defined services: [docker compose](http://docs.docker.com/compose)

<div class="columns">

<div class="column column-5">

```yaml
# simple-compose.yaml
services:
  api:
    build: {context: ./app}
    ports:
      - 8000:8000/tcp

  db:
    image: postgres:12
    environment:
      POSTGRES_DB: demo
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: demo
    volumes:
      - ./modes/api/db/files/docker-entrypoint-initdb.d:/docker-entrypoint-initdb.d


```

</div>

<div class="column column-3">
<img src="demo-composition-direct.png" style="width: 100%"/>
</div>

</div>

<br>
<br>

#### DB schema and seed data
<!-- .element class="fragment" data-fragment-index="1" -->

<div class="columns fragment" data-fragment-index="1">

<div class="column column-1">

```sql
-- app-01-schema.sql
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    version INTEGER DEFAULT 0
);


```

</div>
<div class="column column-1">

```sql
-- app-02-users.sql
INSERT INTO users (name, email, version) VALUES
('Alice', 'alice@example.com', 0),
('Bob', 'bob@example.com', 0);


```

</div>
</div>

Notes:
- answer: docker-compose
- here is our simplified essentials defined using docker-compose
    - api: builds from a directory, exposes port 8000 to the world
    - db: generic postgres image, with DB setting
    - volume mount of a directory that defines schema and seed data in
      a nearly data-defined way.

-----

### Demo: docker compose

Notes:
- [next page]

---

<!-- .slide: class="videoslide" --><video id="video1" autoplay muted></video>

Notes:

<pre>
docker compose -f simple-compose.yaml up --force-recreate  # LEFT
CURL localhost:8000/users  # RIGHT
CURL localhost:8000/users -d '{"name":"Kay","email":"k@example.com"}'  # RIGHT
CURL localhost:8000/users  # RIGHT
# Can see API requests hitting the API service
Ctrl-C  # LEFT
</pre>


-----

<br>

### Data-defined service composition
<!-- .slide: class="fullslide" -->

- docker compose override / overlay
- deep merge multiple compose files

Notes:

- docker compose has an override or overlay capability where you can
  merge multiple compose files.

---

#### compose overlays (override)
<!-- .slide: class="wideslide" -->

- docker compose supports merging of multiple compose files

<div class="columns">

```yaml
services:
  foo:
    image: foo:latest

  bar:
    image: bar:latest
    environment:
      - A=1
      - B=2
```
<!-- .element class="column" -->

<div>+</div> <!-- .element class="column huge-symbol" -->

```yaml
services:
  foo:
    image: foo:2.0.0

  bar:
    environment:
      - B=two
      - C=three
```
<!-- .element class="column" -->

</div>

<div>&darr;</div> <!-- .element class="huge-symbol" -->

```yaml
services:
  foo:
    image: foo:2.0.0

  bar:
    image: bar:latest
    environment:
      A: "1"
      B: two
      C: three


```

Notes:

- here is an example two compose files are being merged together
    - essentially a deep merge with array values appending
        - the image key is just overridden
        - environments and volumes are merged as a map even if defined as a list
- compose overlays have limitations:
  - Unwieldy once you have more than 2 files.
  - No way to represent dependencies.

---

#### [mdc](https://github.com/lonocloud/conlink/blob/master/mdc) (modular docker compose)
<!-- .slide: class="fullslide" -->

<div class="columns">
  <div class="column column-5">
    <img src="demo-mdc-1-modes.png"  width=95% class="fragment overlap" data-fragment-index="1" /> 
    <img src="demo-mdc-2-deps.png"   width=95% class="fragment overlap" data-fragment-index="2" />
    <img src="demo-mdc-3-direct.png" width=95% class="fragment overlap" data-fragment-index="3" />
    <img src="demo-mdc-4-static.png" width=95% class="fragment overlap" data-fragment-index="4" />
    <img src="demo-mdc-5-dhcp.png"   width=95% class="fragment overlap" data-fragment-index="5"/>
  </div>
  <div class="column column-3">
    <img src="demo-composition-empty.png"  width=100% class="fragment overlap" data-fragment-index="1" />
    <img src="demo-composition-empty.png"  width=100% class="fragment overlap" data-fragment-index="2" />
    <img src="demo-composition-direct.png" width=100% class="fragment overlap" data-fragment-index="3" />
    <img src="demo-composition-static.png" width=100% class="fragment overlap" data-fragment-index="4" />
    <img src="demo-composition-dhcp.png"   width=100% class="fragment overlap" data-fragment-index="5" />
  </div>
</div>

Notes:

- mdc
    - allows us to define groups of services as modules and then
      we can "compose" them together.
    - [Show modules] Here is collection of modules. Each module is
      a directory containing a docker compose file and other files
      that define the module.
    - [Show deps] mdc includes a dependency resolution mechanism
      (might be hard to see dotted lines)
    - [Show direct] if the user uses mdc to select the direct module,
      then transitive dependencies on the api and conlink modules are
      pulled in
        - on the right you can see that this selects the essential
          functionality parts of the demo system
- So mdc gives us a data-defined way to manage service groups and
  compose them together.
- However, the next problem we need to talk about is docker compose
  networking; it is very limited when it comes to testing more complex
  production networks which leads us to:

-----

### Data-defined networks

#### [conlink](https://github.com/lonocloud/conlink)

- docker compose networking limitations:
    - basically layer 3 (IP) only
    - simplistic / flat view of networks and IP ranges
    - lack of control over interface naming or order
    - service replica scaling prevents use of user-assigned IPs or
      MACs
- conlink:
    - easy to add to existing docker compose
    - arbitrary data-defined L2 and L3 networking
    - overlay / merge from multiple sources
    - dynamic container scale/replicas
    - external connectivity: tunnels, macvlan, ipvlan
    - network impairments (delay, drops, corruption, etc)

Notes:

- docker compose has major networking limitations
    - basically layer 3 (IP) only
    - simplistic / flat view of networks and IP assignment
    - no control over interface naming or order
    - scaling service replicas prevents use of user-assigned IPs or
      MAC addresses or port forwarding
- [READ]
- network configuration is defined either inline (x-network)
  or separate network configuration files:
    - different compose files and network files are merged
      together in dependency order.
    - later definitions can override or extend earlier ones.

---

### enabling conlink

conlink boilerplate:

```yaml
services:
  conlink:
    image: lonocloud/conlink:2.5.3
    cap_add: [SYS_ADMIN, NET_ADMIN, SYS_NICE, NET_BROADCAST, IPC_LOCK,
              SYS_PTRACE, NET_RAW, SYS_RAWIO, SETUID, SETGID]
    security_opt: [ 'apparmor:unconfined' ]
    pid: host
    devices: [ '/dev/net/tun' ] # for ovs-tcpdump
    env_file: ./.env
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock
      - ./:/remote
    working_dir: /remote
    command: /app/build/conlink.js --compose-file ${COMPOSE_FILE:?COMPOSE_FILE must be specified}
```

network configuration:

```yaml
x-network:
  links:
    - {service: foo, bridge: ctrl, dev: eth0}
    ...

services:
  foo:
    image: foo:latest

  bar:
    image: bar:latest
    x-network:
      links:
        - {bridge: ctrl, dev: eth0}
```
<!-- .element class="reduce-font fragment" -->

Notes:
- enabling conlink is as simple as adding this boilerplate to your
  compose definition.
    - standard service with some elevated permissions and access to
      host pid namespace
    - mount the docker socket to get events from host docker engine
- can coexist with normal docker networking
  - but typically you'll want to disable docker networking in your
    other containers for strong isolation from Internet at runtime.

---

### conlink + mdc
<!-- .slide: class="fullslide" -->

<div class="columns">
  <div class="column column-5">

```yaml
links:
  - {service: api,           bridge: ctrl,     dev: ctl0, ip: 10.0.0.1/16}
  - {service: db,            bridge: ctrl,     dev: ctl0, ip: 10.0.1.1/16}

  - {service: api,           bridge: external, dev: ext0, ip: 10.1.0.1/16, forward: ["8001:8000/tcp"]}
```
<!-- .element class="reduce-font fragment overlap" data-fragment-index="1" -->
```yaml
links:
  - {service: api,           bridge: ctrl,     dev: ctl0, ip: 10.0.0.1/16}
  - {service: db,            bridge: ctrl,     dev: ctl0, ip: 10.0.1.1/16}

  - {service: api,           bridge: external, dev: ext0, ip: 10.1.0.1/16}

  - {service: balancer,      bridge: ctrl,     dev: ctl0, ip: 10.0.1.2/16, forward: ["8000:80/tcp"]}
  - {service: balancer,      bridge: external, dev: ext0, ip: 10.1.1.2/16}
  - {service: message-bus,   bridge: ctrl,     dev: ctl0, ip: 10.0.1.4/16}

  - {service: static-config, bridge: ctrl,     dev: ctl0, ip: 10.0.1.5/16}
```
<!-- .element class="reduce-font fragment overlap" data-fragment-index="2" -->
```yaml
links:
  - {service: api,           bridge: ctrl,     dev: ctl0, ip: 10.0.0.1/16}
  - {service: db,            bridge: ctrl,     dev: ctl0, ip: 10.0.1.1/16}

  - {service: api,           bridge: external, dev: ext0, ip: 10.1.0.1/16}

  - {service: balancer,      bridge: ctrl,     dev: ctl0, ip: 10.0.1.2/16, forward: ["8000:80/tcp"]}
  - {service: balancer,      bridge: external, dev: ext0, ip: 10.1.1.2/16}
  - {service: message-bus,   bridge: ctrl,     dev: ctl0, ip: 10.0.1.4/16}

  - {service: dhcp-server,   bridge: ctrl,     dev: ctl0, ip: 10.0.1.3/16}
  - {service: dhcp-server,   bridge: external, dev: ext0, ip: 10.1.1.3/16}
```
<!-- .element class="reduce-font fragment overlap" data-fragment-index="3" -->
  </div>
  <div class="column column-3">
    <img src="demo-composition-direct.png" width=100% class="fragment overlap" data-fragment-index="1"/>
    <img src="demo-composition-static.png" width=100% class="fragment overlap" data-fragment-index="2"/>
    <img src="demo-composition-dhcp.png"   width=100% class="fragment overlap" data-fragment-index="3"/>
  </div>
</div>

Notes:

-----

### Data-defined system status

#### [dcmon](https://github.com/lonocloud/dcmon)

<img src="dcmon-dhcp-screenshot.png">


Checks are data-defined:

```yaml
# modes/balanced/checks.yaml
settings:
  finished: {balancer: "curl api"}

checks:
  balancer:
    - {id: "proxy up",  regex: "Loading success"}
    - {id: "nats up",   regex: "Information for Stream events created"}
    - {id: "nats msg",  regex: "NATS message"}
    - {id: "reloading", regex: "Reloading HAProxy"}
    -
    - {id: "curl api",  cmd: "curl --fail http://localhost:80/users",
                        deps: {balancer: "reloading"}}
```

Notes:

- useful for visual feedback
- data defined checks within each module that are merged together
- optional finished settings are useful for testing up condition
    - each module can define own finished conditions
    - only when conditions from all selected modules have completed
      will dcmon exit
- also has event per line output

-----

### Demo: compose, mdc, conlink, dcmon

Notes:
- [next page]

---
<!-- .slide: class="videoslide" --><video id="video2" autoplay muted></video>

Notes:

- DEMO part 1
  - direct:
    - mdc direct
    - dcmon in one window, then `up --force-recreate`
    - CURL localhost:8001/users
    - dc logs -f, describe logs
    - do scale up
        - see conlink create both interfaces (north and south)
    - CURL localhost:8002/users  # different port, different api
        - shows conlink assigning incrementing IP based on scale
    - leave up!
  - static:
    - mdc static
    - dcmon in one window, then `up --force-recreate`
    - dc logs -f, then `CURL localhost:8000/users`
    - dc exec conlink tshark -nli any not ip6
    - CURL localhost:8000/users
    - down   # important!
  - dhcp:
    - mdc dhcp, up -d, dcmon
    - CURL localhost:8000/users
    - dcenter -n db:ctl0 dhcp-server:ext0 message-bus:ctl0 balancer:ctl0 -- tshark -nli {1} not ip6
    - do api scale up
        - shows backend traffic: DB, DHCP, NATS
        - importantly, single host system so no timestamp skew
    - leave up!

---
<!-- .slide: class="videoslide" --><video id="video3" autoplay muted></video>

Notes:

- DEMO part 2
    - start up separate independent config to monitor the direct and
      dhcp configs that are still running:
    - mdc monitor, dc up
    - [switch to present head and show grafana]
        - see about 5ms of latency in normal operation
    - [switch to slides]
        - `cat modes/latency/compose.yaml`
        - restart dhcp with `mdc dhcp,latency`
    - [switch to present head and show grafana]
        - seem about 25ms latency
    - [switch to slides]
        - `cat modes/drops/compose.yaml`
        - restart dhcp with `mdc dhcp,drops`
    - [switch to present head and show grafana]
        - latency spikes to 100ms
    - [switch to slides]
        - `cat modes/drops/compose.yaml`
        - restart dhcp with `mdc dhcp,drops`

-----

### Data-defined testing

#### [instacheck](https://github.com/kanaka/instacheck)

* generative testing (Property-based Testing)
  * define how to generate tests
    * instacheck defines tests using EBNF
  * define how to validate results
    * Oracle problem
      * Use the simpler configuration as the Oracle! <!-- .element class="fragment" -->

Notes:

- generative testing or property-based testing
  - may also hear it described as QuickCheck which was the Haskell
    tool that popularized the approach
  - define how to generate tests rather than defining each
    individual test
    - instacheck defines tests using EBNF
        - rather than traditional approach of defining code generators
  - define how to validate results
    - Oracle problem: a way to determine if the results are correct
      given the generated test.
    - For complex systems this can be difficult and you can often end
      up defining a whole parallel model for how the system works.
    - Our solution: for this type of problem where the problem is
      often that things work in the small but don't when deployed in
      production, we have an answer.
        - Use the simpler deployment as the Oracle!

---

#### Data-defined generative tests (EBNF)

```ebnf
(* actions.ebnf *)

requests   = '[' request ( ',\n ' request )* ']\n'
request    = '{"method":"POST",'   '"path":"/users",'     '"payload":' post-user '}'
           | '{"method":"PUT",'    '"path":"/users/' id '","payload":' put-user '}'  (* {:weight 500} *)
           | '{"method":"DELETE",' '"path":"/users/' id '"}'
           | '{"method":"GET",'    '"path":"/users/' id '"}'
           | '{"method":"GET",'    '"path":"/users"}'
post-user  = '{"name":"' name '","email":"' email '"}'
put-user   = post-user
           | '{"name":"' name '"}'
           | '{"email":"' email '"}'
id         = "1" (* {:weight 1000} *)
           | "2" (* {:weight 500} *)
           | "3"
           | "4"
           | "5"
           | "6"
           | "7"
           | "8"
           | "9"
name       = 'Joe ' #"[A-Z]"
email      = 'joe' #"[0-9]" '@example.com'


```

---

```shell
$ gentest samples
$ cat output/samp-00*
```

```json
[{"method":"PUT","path":"/users/5","payload":{"name":"Joe F","email":"joe8@example.com"}},
 {"method":"GET","path":"/users/8"}]
[{"method":"PUT","path":"/users/1","payload":{"email":"joe0@example.com"}}]
[{"method":"PUT","path":"/users/9","payload":{"name":"Joe I","email":"joe8@example.com"}},
 {"method":"POST","path":"/users","payload":{"name":"Joe J","email":"joe0@example.com"}},
 {"method":"GET","path":"/users"},
 {"method":"PUT","path":"/users/4","payload":{"name":"Joe L","email":"joe2@example.com"}}]
[{"method":"PUT","path":"/users/4","payload":{"email":"joe6@example.com"}}]
[{"method":"PUT","path":"/users/1","payload":{"email":"joe2@example.com"}},
 {"method":"GET","path":"/users"},
 {"method":"PUT","path":"/users/3","payload":{"name":"Joe F","email":"joe6@example.com"}},
 {"method":"PUT","path":"/users/1","payload":{"email":"joe6@example.com"}},
 {"method":"PUT","path":"/users/1","payload":{"name":"Joe D","email":"joe5@example.com"}},
 {"method":"POST","path":"/users","payload":{"name":"Joe I","email":"joe4@example.com"}}]
...
```


Notes:

- Not just static tests, but PBT/generative tests in data-defined
  way
- quick overview of instacheck
- oracle problem
    - Scenario: Bug that only happens in full scaled production
      deployments. We suspect a bug that happens with certain
      sequences of requests but nobody has been able to pinpoint the
      issue yet and it seems a bit non-deterministic.
    - We want to try generative property-based testing, but the
      problem is when we generate a request sequence, what is the
      right answer?
    - Well in this particular scenario where we have a bug that only
      happens in production, we have an Oracle: the simpler
      configuration.

-----

### Demo: instacheck/gentest

Notes:
- [next page]

---
<!-- .slide: class="videoslide" --><video id="video4" autoplay muted></video>


Notes:

- DEMO
    - start up direct and dhcp instances [restart dhcp]
        - we have two instances running now. The full system and the
          simpler direct instance as a test Oracle.
    - show success test
      `gentest check http://localhost:8000 http://localhost:8001`
        - show samples
    - add bug module and restart dhcp
    - show testing again
        - run a few times until failure
            - describe result output
        - use run command with sample
          `gentest run http://localhost:8000 http://localhost:8001 output/sample-final` 
    - load weights from existing failures for faster reproduction
      `gentest parse --weights output/weights.edn http://localhost:8000 http://localhost:8001`
        - show faster/better reproduction
        - capture/show response logs and what is actually
          different
    - show parse --ebnf-output with EBNF rewritten to remove paths
      with 0 weight. Re-run check with new EBNF file.

-----

### Data-defined test suites

#### [dctest](https://github.com/viasat/dctest)

Notes:

- ...
- show running in CI

-----

### Tool limitations

(invitation to get involved)
<!-- .element class="reduce-font" -->

- conlink:
    - interfaces appear after the service starts
- mdc
    - algorithmically inneficient
- dcmon:
    - only monitors good conditions
- clj-protocol is not data-defined enough
    - intermingled code
    - expression problem
- instacheck
    - limitations around direct and mutual recursion
    - Clojure only (no ClojureScript support yet)
- dctest:
    - early in development
    - needs fixtures, parameterization, etc

Notes:

- conlink:
    - interfaces appear after the service starts
        - this is reason for the wait.sh script
        - often also need to have custom scripts that wait for
          seeding, schema, migrations, service readiness, etc.
        - currently working on a full event system. Replace
          wait.sh script with static binary that implements full
          event system for startup coordination.
- mdc
    - algorithmically inneficient (order n^2)
- dcmon only monitors good conditions
    - needs warnings and error events too
- clj-protocol is not data-defined enough
    - intermingled code
    - expression problem:
        - can't currently extend protocol except accretively
- instacheck
    - inherits from test.check and has same limitations around
      direct and mutual recursion.
    - currently Clojure only so not directly integratable into
      dctest which is currently clojurescript.
- dctest is early.
    - fixtures.
    - more declarative actions (rather that shell snippets)
    - integrate with openapi perhaps?
    - integrate property-based testing and instacheck

-----

<div class="columns">
  <div class="column column-5">

#### Links

* This Presentation: [kanaka.github.io/data-defined-systems/conj-2024](https://kanaka.github.io/data-defined-systems/conj-2024)
* Demo System: [github.com/kanaka/data-defined-systems](https://github.com/kanaka/data-defined-systems)
* Projects:
  * conlink: [lonocloud.github.io/conlink/](https://lonocloud.github.io/conlink/)
  * mdc: [github.com/lonocloud/conlink/blob/master/mdc](https://github.com/lonocloud/conlink/blob/master/mdc)
  * resolve-deps: [github.com/viasat/resolve-deps](https://github.com/viasat/resolve-deps)
  * dcmon: [github.com/lonocloud/dcmon](https://github.com/lonocloud/dcmon)
  * clj-protocol: [github.com/lonocloud/clj-protocol](https://github.com/lonocloud/clj-protocol)
  * instacheck: [github.com/kanaka/instacheck](https://github.com/kanaka/instacheck)
  * dctest: [viasat.github.io/dctest](https://viasat.github.io/dctest)

  </div>
  <div class="column column-1">
    &nbsp;
  </div>
  <div class="column column-3">

#### Project Contributors:

  * Aaron Brooks (Equinix)
  * Joel Martin (Equinix)
  * Jon Smock (Viasat)
  * Greg Warner (Viasat)

  </div>
</div>

-----

### Extra Slides

-----

### Other stuff

- net2dot
- dcenter
- wait.sh
- copy.sh

-----
<!-- .slide: class="videoslide" --><video id="video5" autoplay muted></video><br/>
<button class="start-share-screen" data-targets="video0 video1 video2 video3 video4 video5">Start Share</button>

-----

### Slide with shared screen

<ul>
<video id="video6" style="float:right; height:30%; width:30%;" autoplay muted></video>
<li> bullet 1 </li> <!-- .element class="fragment" -->
<li> bullet 2 </li> <!-- .element class="fragment" -->
<li> bullet 3 </li>  <!-- .element class="fragment" -->
</ul>

-----
<!-- .slide: class="videoslide" --><video id="web1" autoplay muted></video>

Notes:
* another notes area for web1 video element

