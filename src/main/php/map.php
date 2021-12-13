<?php require 'global/header.php';

if (!isset($player)) die();

$data = POST('entities', [
        'refDataFilters' => [
            [ 'key' => 'galaxy', 'value' => '1' ],
            [ 'key' => 'system', 'value' => '1' ],
        ]]);

render_header();
?>

<?print_r($data)?>

<?php require 'global/footer.php'; ?>