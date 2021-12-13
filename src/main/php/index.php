<?php require 'global/header.php'; 

if ($_POST) {
    $response = PUT('public/player');
    $_SESSION['entity_id'] = $response['entity_id'];
} 

if (isset($_SESSION['entity_id'])) {
    header('Location: game.php');
    die();
}

render_header();
?>

<div class="row">
<div class="col-md-3"></div>
<div class="col-md-6">
<div class="card">
<div class="card-body">
<form method="post">
    <button class="btn btn-primary btn-block" name="create">create player</button>
</form>
</div>
</div>
</div>
</div>


<?php require 'global/footer.php'; ?>