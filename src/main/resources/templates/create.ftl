<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <title>支付</title>
</head>

<body>
<#-- <div>内容</div> 不要回车等操作,里面的所有都是内容,包括回车,换行,空格!!!-->
<div id="myQrcode"></div>
<div id="orderId">${orderId}</div>
<div id="returnUrl">${returnUrl}</div>

<script src="https://cdn.bootcdn.net/ajax/libs/jquery/1.5.1/jquery.min.js"></script>
<script src="https://cdn.bootcdn.net/ajax/libs/jquery.qrcode/1.0/jquery.qrcode.min.js"></script>
<script>
    jQuery('#myQrcode').qrcode({
        text    :"${codeUrl}"
    });

    $(function () {
        //定时器
        setInterval(function () {
            console.log('开始查询支付状态...')
            console.log($('#orderId').text())

            $.ajax({
                url: '/pay/queryByOrderId',
                data: {
                    'orderId': $('#orderId').text()
                },
                success: function (result) {
                    console.log(result)
                    if (result.platformStatus != null
                        && result.platformStatus === 'SUCCESS') {
                        location.href = $('#returnUrl').text()
                    }
                },
                error: function (result) {

                    alert(result + "|" + $('#orderId').text())
                }

            })
        }, 2000)
    });
</script>
</body>
</html>