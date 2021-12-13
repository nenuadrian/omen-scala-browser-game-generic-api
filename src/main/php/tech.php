<?php require 'global/header.php';

if (!isset($player)) die();

$data = GET('tech-tree');

render_header();
?>

<?print_r($data)?>

<?php require 'global/footer.php'; ?>