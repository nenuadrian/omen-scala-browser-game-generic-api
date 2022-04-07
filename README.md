# Omen Engine

A backend server API, driven mainly by configuration (YAML), which allows you to define the relationships between entities of your game and leverage the API in a thin UI layer on top (that you need to create). 

This means you can focus on the user experience and gameplay, and less on the back-end models, storage and scalability.

The engine is capable of validating your config, providing visualisations of the relationships between entities, processing formulas, and much more.

The system is pretty much an entity management system, where relationships can be defined between them, and formulas uses to derive attributes based on this hierarchy.

## Sample YAML game definition

[Click here for sample from the codebase](./src/main/resources/game_configs/space.yaml)

The sample focuses on creating a space simulation browser based text game.

## Example game

An example [game UI has been implemented in PHP in this codebase](./src/main/php).

The game uses the space sample config, and leverages PHP as a trusted middle layer between the raw Omen API and the UI. The game-play/business logic is implemented in PHP, leveraging the API to achieve persistence and many of the generic functionalities.

## API

### Responses
All responses are in this JSON format:

```
{ "status": 200, "data": {} }
```

```
{ "status": 200, "error": "ERROR_MESSAGE" }
```

### Endpoints

| ENDPOINT                                     | ACTION | PARAMETERS                                     | DESCRIPTION                                         |
|----------------------------------------------|--------|------------------------------------------------|-----------------------------------------------------|
| ping                                         | GET    |                                                | Check status of server                              |
| configuration                                | GET    |                                                | JSON version of the YAML configuration              |
| public/player                                | PUT    |                                                | JSON version of the YAML configuration              |
| entities                                     | POST   |                                                | Find entities via a EntitiesQuery                   |
| entities                                     | PUT    |                                                | Create Entity                                       |
| entities/{entityId}                          | GET    | primaryParentEntityId?                         | Get entity info                                     |
| entities/{entityId}/upgrade                  | POST   | primaryParentEntityId?                         | Upgrade entity level                                |
| entities/{entityId}/ref/{key}/{value}        | POST   | primaryParentEntityId?                         | Update entity ref data                              |
| entities/{entityId}/attributes/{key}/{value} | POST   | primaryParentEntityId?                         | Update entity attributes                            |
| entities/{entityId}/requirements             | GET    | primaryParentEntityId?                         | Compute requirements and whether they are fulfilled |
| entities/{entityId}/requirements             | POST   | primaryParentEntityId? / amount?               | Apply attributes of fulfilled requirements          |
| tasks                                        | PUT    |                                                | Create Tasks                                        |
| tasks                                        | GET    |                                                | Create Task                                         |
| tasks/{taskId}/ack                           | POST   |                                                | Acknowledge task completion                         |
| leaderboard                                  | GET    | id                                             | Get leaderboard by a specific type                  |
| tech-tree                                    | GET    | player_id? / parent_entity_id? / acknowledged? | Tries to generate a graph from the config           |

## Tests

There are tests for most of the functionality, mocking the entire database and simulating a player interracting with the system given a sample game configuration.

## Goals & Features
  * develop a game theme agnostic system for building browser based 2D games, not based on dialog, but on time sensitive tasks (building, upgrading, attacking, researching, farming)
  * allow for a scripting language which can define a game end to end with minimal programming, allowing for the description of the entire game economy (formulas, relations between entities, validation of API queries)
  * abstract away the need to account for thread safety, ranking, data-storage, handling of computations from the game development process, leaving the most important part (economy dynamics, story, graphics and UI) at the heart of the problem
  * automated testing which guarantees the inner working of the underlying engine
  
## Visualize the tech-tree and game config

An HTML endpoint is available which renders a visualisation of the entire config by using the `localhost:8083/tech-tree` endpoint at `localhost:8083/tech-tree/html`

![tech](docs/diagram-tech-tree.png)
