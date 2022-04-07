<?php 

$startTime = microtime(true);
$apiCalls = 0;

ini_set('display_errors', 1);
ini_set('display_startup_errors', 1);
error_reporting(E_ALL);

require 'vendor/autoload.php';
$host = 'http://localhost:8083/';

session_start();

function GET($url) {
    global $host, $apiCalls;
    $response = \Httpful\Request::get($host . $url)
     ->expectsJson()
                        ->send();
    $apiCalls++;
    $data = json_decode(json_encode($response->body), true);
    if (!isset($data['data'])) {
        throw new Exception("Undefined data: " . json_encode($response->body));
    }
    return $data['data'];
}

function POST($url, $data = []) {
    global $host, $apiCalls;
    $response = \Httpful\Request::post($host . $url)
     ->expectsJson()->sendsJson();
    if ($data) $response = $response->body($data);
    $response = $response->send();
    $apiCalls++;
    $data = json_decode(json_encode($response->body), true);
    if ($data['status'] != 200 && !isset($data['data'])) {
        throw new Exception("Undefined data: " . var_export($response->body));
    }
    return $data['data'];
}

function PUT($url, $data = []) {
    global $host, $apiCalls;
    $apiCalls++;
    $response = \Httpful\Request::put($host . $url)
        ->expectsJson()
        ->sendsJson();
    if ($data) $response = $response->body($data);
    $response = $response->send();
    $data = json_decode(json_encode($response->body), true);
    return $data['data'];
}

function render_header() {
    ?>
        <head>
        <title>OMEN</title>
        <link rel="stylesheet" href="https://bootswatch.com/4/sketchy/bootstrap.css">
        <script src="https://code.jquery.com/jquery-3.5.1.slim.min.js" integrity="sha384-DfXdz2htPH0lsSSs5nCTpuj/zy4C+OGpamoFVy38MVBnE+IbbVYUew+OrCXaRkfj" crossorigin="anonymous"></script>
        <script src="https://cdn.jsdelivr.net/npm/popper.js@1.16.0/dist/umd/popper.min.js" integrity="sha384-Q6E9RHvbIyZFJoft+2mJbHaEWldlvI9IOYy5n3zV9zzTtmI3UksdQRVvoxMfooAo" crossorigin="anonymous"></script>
        <script src="https://stackpath.bootstrapcdn.com/bootstrap/4.5.0/js/bootstrap.min.js" integrity="sha384-OgVRvuATP1z7JjHLkuOU7Xw704+h835Lr+6QL9UvYjZE3Ipu6Tp75j7Bh/kR0JKI" crossorigin="anonymous"></script>
        <style>
        form { margin-bottom:0; }
        </style>
        </head>
        <div class="container">
            <?php if (isset($_SESSION['entity_id'])): ?>
        <br/>
            <nav class="navbar navbar-expand-lg navbar-light bg-light">
                <a class="navbar-brand" href="#">SPACE</a>
                <button class="navbar-toggler" type="button" data-toggle="collapse" data-target="#navbarSupportedContent" aria-controls="navbarSupportedContent" aria-expanded="false" aria-label="Toggle navigation">
                    <span class="navbar-toggler-icon"></span>
                </button>

                <div class="collapse navbar-collapse" id="navbarSupportedContent">
                    <ul class="navbar-nav mr-auto">
                        <li class="nav-item active">
                            <a class="nav-link" href="index.php">Home</a>
                        </li>
                        <li class="nav-item active">
                            <a class="nav-link" href="map.php">Map</a>
                        </li>
                        <li class="nav-item active">
                            <a class="nav-link" href="tech.php">Tech</a>
                        </li>
                        <li class="nav-item">
                            <a class="nav-link" href="organization.php">Organization</a>
                        </li>
                        <li class="nav-item">
                            <a class="nav-link" href="leaderboard.php">Leaderboard</a>
                        </li>

                        <li class="nav-item">
                            <a class="nav-link" href="leaderboard.php?for=organizations">Org Leaderboard</a>
                        </li>
                    </ul>
                    <a class="btn btn-outline-warning my-2 my-sm-0" href="logout.php">X</a>
                </div>
            </nav>
            <?php if (isset($_GET['planet'])): ?>
            <br/>
            <div class="row">
                <div class="col-md-3"></div>
                <div class="col-md-6">
                    <nav class="navbar navbar-expand-lg navbar-light bg-light">
                        <button class="navbar-toggler" type="button" data-toggle="collapse" data-target="#navbarSupportedContent" aria-controls="navbarSupportedContent" aria-expanded="false" aria-label="Toggle navigation">
                            <span class="navbar-toggler-icon"></span>
                        </button>

                        <div class="collapse navbar-collapse" id="navbarSupportedContent">
                            <ul class="navbar-nav mr-auto">
                                    <li class="nav-item active">
                                        <a class="nav-link" href="planet.php?planet=<?=$_GET['planet']?>">Overview</a>
                                    </li>
                                    <li class="nav-item active">
                                        <a class="nav-link" href="planet.php?for=building&planet=<?=$_GET['planet']?>">Buildings</a>
                                    </li>
                                    <li class="nav-item active">
                                        <a class="nav-link" href="planet.php?for=research&planet=<?=$_GET['planet']?>">Research</a>
                                    </li>
                            </ul>
                        </div>
                    </nav>
                </div>
            </div>
            <?php endif; ?>

            <br/>
            <?php endif; ?>
            <br/>
    <?php
}

function renderAttributes($entity_id, $attributes) {
    ?>
            <h3>Attributes</h3>
            <div class="card">
    <div class="card-body">
            <?php
            foreach($attributes as $k => $att): ?>
            <?=$k ? '<br/>' : ''?>
                <form method="post">
                    <div class="row">

                        <div class="col-md-3">
                            <?=$att['attr']?>
                        </div>
                        <div class="col-md-6">
                            <input type="text" value="<?=$att['value']?>" class="form-control" name="value"/>
                            <input type="hidden" value="<?=$entity_id?>" class="form-control" name="entity_id"/>
                        </div>
                        <div class="col-md-3">
                            <button class="btn btn-primary btn-block" name="attribute" value="<?=$att['attr']?>">Update</button>
                        </div>
                    </div>

                </form>

            <?php endforeach;?>
    </div>
            </div>
            <?php
}

function renderRefData($entity_id, $refData) {
    ?>
            <h3>Ref</h3>
            <div class="card">
    <div class="card-body">
            <?php
            foreach($refData as $k => $att): ?>
            <?=$k ? '<br/>' : ''?>
                <form method="post">
                    <div class="row">

                        <div class="col-md-3">
                            <?=$att['ref_key']?>
                        </div>
                        <div class="col-md-6">
                            <input type="text" value="<?=urldecode($att['ref_value'])?>" class="form-control" name="value"/>
                            <input type="hidden" value="<?=$entity_id?>" class="form-control" name="entity_id"/>
                        </div>
                        <div class="col-md-3">
                            <button class="btn btn-primary btn-block" name="refKey" value="<?=$att['ref_key']?>">Update</button>
                        </div>
                    </div>

                </form>

            <?php endforeach;?>
    </div>
            </div>
            <?php
}

try {
    GET('ping');
} catch (Exception $ex) {
    render_header();
    die('<div class="alert alert-danger">Could not connect to server: ' . $host . "</div>");
}


if (isset($_SESSION['entity_id'])) {
    try {
        $player = GET('entities/' .  $_SESSION['entity_id']);
    } catch (Exception $e) {
        header('Location: logout.php');
        die();
    }
    $org = array_filter($player['refData'], function ($e) { return $e['ref_key'] == 'organization'; });
    $org = $org ? $org[0]['ref_value'] : null;
    if ($org) {
        $org = GET('entities/' . urldecode($org));
    }
    if (isset($_POST['attribute'])) {
            $response = POST('entities/' . $_POST['entity_id'] . '/attributes/' . $_POST['attribute'] . '/' . $_POST['value'] . '?');
            header('Location: ' . $_SERVER['REQUEST_URI']);
            die();
        }

        if (isset($_POST['refKey'])) {
            $response = POST('entities/' . $_POST['entity_id'] . '/ref/' . $_POST['refKey'] . '/' . urlencode($_POST['value']) . '?');
            header('Location: ' . $_SERVER['REQUEST_URI']);
            die();
        }

} else {
    if ($_SERVER['REQUEST_URI'] != '/index.php') {
        header('Location: index.php');
    }
}

$configuration = GET('configuration');
