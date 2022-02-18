function correctDateTime(selector) {
    let dateTimeJQ = $(selector)

    let dateTime = dateTimeJQ.val()
    // already standard format
    if (/.+T.+Z$/.test(dateTime)) {
        return
    }

    let utc = moment(dateTime, 'YYYY-MM-DD hh:mm').utc().format()
    dateTimeJQ.val(utc)
}
