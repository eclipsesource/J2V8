# source: https://stackoverflow.com/a/24431423

$source = $args[0]
$destination = $args[1]
$cookies = $args[2]

$client = new-object System.Net.WebClient

if ($cookies -ne $null)
{
    $client.Headers.Add([System.Net.HttpRequestHeader]::Cookie, $cookies)
}

$client.downloadFile($source, $destination)
