$(function() {
    $('#timezone').val(new Date().getTimezone())

    let d = moment().format('YYYY-MM-DD')
    $('#startTime').datetimepicker({
        inline: false,
        format: 'Y-m-d H:i',
        formatDate: 'Y-m-d'
    }).val(d + ' 00:00')

    let deadTimeJQ = $('#deadTime')
    // let deadTime = deadTimeJQ.val()
    // deadTime = moment().format(deadTime, 'YYYY-MM-DD')
    deadTimeJQ.datetimepicker({
        inline: false,
        timepicker: false,
        format: 'Y-m-d H:i',
        formatDate: 'Y-m-d'
    })

    $('.cronExpression').hide()

    $('#period').change(function() {
        let value = $('#period').val()
        console.log(value)
        if (value === 'PT-1H') {
            $('.cronExpression').show()
        } else {
            $('.cronExpression').hide()
        }
    })

    $('#cronExpressionBuilder').cronBuilder({
        onChange: function(expression) {
            $('#cronExpression').val(expression);
        }
    });

    $('input[type=submit]').click(function(e) {
        correctDateTime('#startTime')
        correctDateTime('#deadTime')
    })
});
