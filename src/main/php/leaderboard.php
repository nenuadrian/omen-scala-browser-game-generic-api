<?php require 'global/header.php';

if (!isset($player)) die();

$data = GET('leaderboard?id=' . (isset($_GET['for']) ? $_GET['for'] : 'players'));

render_header();
?>

<?print_r($data)?>

<?php require 'global/footer.php'; ?>