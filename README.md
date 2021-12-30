# Omen Engine

## sample game definition

[Click here for sample from the codebase](./src/main/resources/game_configs/space.yaml)

## API

WIP

| ENDPOINT | ACTION | PARAMETERS | DESCRIPTION  |
|----------|--------|------------|-----|
|          |        |            |     |

## goals
  * develop a game theme agnostic system for building browser based 2D games, not based on dialog, but on time sensitive tasks (building, upgrading, attacking, researching, farming)
  * allow for a scripting language which can define a game end to end with minimal programming, allowing for the description of the entire game economy (formulas, relations between entities, validation of API queries)
  * abstract away the need to account for thread safety, ranking, data-storage, handling of computations from the game development process, leaving the most important part (economy dynamics, story, graphics and UI) at the heart of the problem
  * automated testing which guarantees the inner working of the underlying engine


## Visualize the tech-tree and game config

By using the tech-tree endpoint you can place the data in the following HTML page and obtain this type of visualisation of what the engine knows about your game

![tech](docs/diagram-tech-tree.png)

```
<script type="text/javascript" src="https://visjs.github.io/vis-network/standalone/umd/vis-network.min.js"></script>
<div id="mynetwork"></div>
<script>
    // create an array with nodes
    var nodes = new vis.DataSet([]);
    
    // create an array with edges
    var edges = new vis.DataSet([]);
    
    // create a network
    var container = document.getElementById("mynetwork");
    var data = {
      nodes: nodes,
      edges: edges,
    };
    var options = {};
    var network = new vis.Network(container, data, options);
</script>

```