<?php require 'global/header.php';

if (!isset($player)) die();

if (isset($_POST['planet'])) {
    $data = PUT('entities', ['id' => 'planets', 'entity_parent' => $player['entity_id'], 'entity_primary_parent' => $player['entity_id']]);

    POST('entities/' . $data['entity_id'] . '/ref/name/' . urlencode('No title'));

    header('Location: game.php');
    die();
}

$data = POST('entities', ['primaryParentEntityId' => $player['entity_id']]);
$planets = array_filter($data, function ($e) { return $e['id'] == 'planets'; });


render_header();
?>

<?php renderRefData($player['entity_id'], $player['refData']); ?>
<Br/>

<?php renderAttributes($player['entity_id'], $player['attributes']); ?>

<br/><Br/>
<h3>Planets</h3>


<form method="post">
    <button class="btn btn-success btn-block" name="planet">Create planet</button>
</form>
<br/>
<?php foreach($planets as $planet):
    $title = urldecode(array_filter($planet['refData'], function ($e) { return $e['ref_key'] == 'name'; })[0]['ref_value']); ?>
    <div class="card">
        <div class="card-header"><?=$title?></div>
    <div class="card-body">

    <div class="row">
    <?php foreach($planet['attributes'] as $att): ?>
        <div class="col-6"><?=$att['attr']?>: <?=$att['value']?></div>
        
        <?php endforeach; ?>
    </div>
    <br/>
        <a class="btn btn-primary btn-block btn-lg" href="planet.php?planet=<?=$planet['entity_id']?>">select planet</a>
    </div></div>
    <br/>
<?php endforeach; ?>

<?php require 'global/footer.php'; ?>