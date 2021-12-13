<?php require 'global/header.php';

$planet = GET('entities/' . $_GET['planet']);

if (isset($planet['status']) && $planet['status'] == 404) {
    die('Planet not found');
}

if (isset($_POST['entity_id'])) {
    $amount = isset($_POST['amount']) ? intval($_POST['amount']) : 1;
    $requirements = POST('entities/' . $_POST['entity_id'] . '/requirements?amount=' . $amount);
    if ($requirements['status'] == 200) {
        $task = PUT('tasks?parent_entity_id=' . $_GET['planet'], ['json' => ['duration' => 1, 'data' => ['entity_id' => $_POST['entity_id']]]]);
        header('Location: ' . $_SERVER['REQUEST_URI']);
        die();
    } else {
        die('Requirements not met for this selection');
    }
}

$tasks = GET('tasks?parent_entity_id=' . $_GET['planet']);

foreach($tasks as $task) {
    if ($task['finished']) {
        $entity_id = $task['data']['entity_id'];
        $entity = GET('entities/' . $entity_id);
        
        POST('entities/' . $entity_id . '/upgrade/' . ($entity['amount'] + 1));
        POST('tasks/' . $task['task_id'] . '/ack');
    }
}

render_header();

$title = urldecode(array_filter($planet['refData'], function ($e) { return $e['ref_key'] == 'name'; })[0]['ref_value']);
function render($entities, $withAmounts = false) { ?>
    <?php foreach($entities as $entity):
        $requirements = GET('entities/' . $entity['entity_id'] . '/requirements');

        ?>
        <div class="card">
        <div class="card-header">
        <?=$entity['id']?> = <?=$entity['amount']?> <?=!$requirements['fulfilled'] ? ' | NOT FULFILLED' : ''?>

    </div>
        <div class="card-body">
        <?php if ($requirements): ?>
            <h4>Requirements</h4>
        <h5>attributes</h5>
        <div class="row">
            <?php foreach($requirements['attributes'] as $att): ?>
                <div class="col-md-6">
                    <p><?=$att['requirement']['id']?>: <?=$att['requirement']['formula']?> (<?=$att['current']?>) <?=!$att['fulfilled'] ? ' | NOT FULFILLED' : ''?></p>
                </div>
            <?php endforeach; ?>
        </div>
        <h5>entities</h5>

        <?php foreach($requirements['entities'] as $att): ?>
            <p><?=$att['requirement']['id']?>: <?=$att['requirement']['formula']?> (<?=$att['current']?>) <?=!$att['fulfilled'] ? ' | NOT FULFILLED' : ''?></p>
            <?php endforeach; ?>
        <?php endif; ?>



        <?php if ($requirements['have']): ?>
            <h4>have</h4>
            <?php foreach($requirements['have'] as $att):  ?>
                <p><?=$att['requirement']['id']?>: <?=$att['requirement']['formula']?></p>
            <?php endforeach; ?>
        <?php else: ?>
            <h4>attr</h4>
            <?php foreach($entity['attributes'] as $att):  ?>
                <p><?=$att['attr']?>: <?=$att['value']?></p>

            <?php endforeach; ?>
        <?php endif; ?>

            <form method="post">
            <input type="hidden" name="entity_id" value="<?=$entity['entity_id']?>"/>
            <input type="hidden" name="id" value="<?=$entity['id']?>"/>
            <?php if ($withAmounts): ?>
                <input type="number" name="amount" class="form-control text-center" style="display:inline-block; max-width:150px" min="1" max="999" value="1"/>
            <?php endif; ?>
            <button class="btn btn-primary" <?=!$requirements['fulfilled'] ? 'disabled' : ''?>>action</button>

            </form>
        </div></div>
        <br/>
    <?php endforeach; ?>

<?php } ?>


    <h1><?=$title?></h1>
    <br/>

<?php if ($tasks): ?>
    <div class="card">
        <div class="card-header">
            Tasks
        </div>
        <div class="card-body">
            <?php print_r($tasks); ?>
        </div>
    </div>
    <br/>

<?php endif; ?>
<?php if (!isset($_GET['for'])): ?>
    <?php renderRefData($planet['entity_id'], $planet['refData']); ?>
    <Br/>
    <?php renderAttributes($planet['entity_id'], $planet['attributes']); ?>
<?php endif; ?>
<Br/><br/>
<?php if (isset($_GET['for'])): ?>
<?php
$stuff = POST('entities', ['tag' => $_GET['for'], 'parent_entity_id' => $_GET['planet']]); ?>
<?php render($stuff, isset($_GET['for']) && $_GET['for'] === 'ship'); ?>
<?php endif; ?>
<?php require 'global/footer.php'; ?>