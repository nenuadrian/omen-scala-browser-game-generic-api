<?php require 'global/header.php';

if (!isset($player)) die();

$data = GET('leaderboard?id=players');

if (isset($_POST['create'])) {
    $data = PUT('entities', ['id' => 'organizations']);

    POST('entities/' . $data['entity_id'] . '/ref/name/' . urlencode('New organization'));
    POST('entities/' . $player['entity_id'] . '/ref/organization/' . urlencode($data['entity_id']));
    header('Location: organization.php');
}

if (isset($org)) {
    $members = POST('entities', [
        'refDataFilters' => [
            [ 'key' => 'organization', 'value' => $org['entity_id'] ]
        ]]);
}
render_header();
?>

<?php if (!isset($org)): ?>
<form method="post">
    <button type="submit" class="btn btn-lg btn-block btn-primary" name="create">create organization</button>
</form>
<?php else: ?>
    <h3>org</h3>
    <?php renderRefData($org['entity_id'], $org['refData']); ?>

    <h3>members</h3>
    <?php foreach($members as $member): ?>
        <?php print_r($member); ?>
    <?php endforeach; ?>
<?php endif; ?>

<?php require 'global/footer.php'; ?>