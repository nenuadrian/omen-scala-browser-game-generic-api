# API

All responses use the same envelope:

```json
{ "status": 200, "data": {} }
```

or:

```json
{ "status": 200, "error": "ERROR_MESSAGE" }
```

## Endpoints

| Endpoint | Method | Description |
| --- | --- | --- |
| `ping` | GET | Health check |
| `configuration` | GET | JSON version of loaded YAML configuration |
| `player` | PUT | Create player entity |
| `entities` | POST | Query entities (`EntitiesQuery`) |
| `entities` | PUT | Create entity |
| `entities/{entityId}` | GET | Get entity info |
| `entities/{entityId}/upgrade` | POST | Upgrade entity level |
| `entities/{entityId}/ref/{key}/{value}` | POST | Update entity reference data |
| `entities/{entityId}/attributes/{key}/{value}` | POST | Update entity attributes |
| `entities/{entityId}/requirements` | GET | Compute requirements and fulfillment |
| `entities/{entityId}/requirements` | POST | Apply attributes of fulfilled requirements |
| `tasks` | PUT | Create task |
| `tasks` | GET | List tasks |
| `tasks/{taskId}` | POST | Acknowledge task completion |
| `leaderboard` | GET | Get leaderboard for a given id/type |
| `tech-tree` | GET | Generate graph-like view from configuration |

For complete parameter details, refer to the source and tests.
