$(function() {
    const width = document.body.clientWidth

    $(".tasks a.preview").click(function () {
        const target = this
        const taskId = this.getAttribute('data-target-id')
        const targetId = 'a' + taskId
        const card = document.getElementById(targetId)
        // console.log($(this).siblings('div.preview').length)
        $(card).removeClass('d-none').dialog({
            title: "Result set for " + targetId,
            minWidth: 0.8 * width,
            beforeClose: function( event, ui ) {
            }
        })
    })
});
