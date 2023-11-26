$(document).ready(function () {
    $("#shortener").submit(function (event) {
        event.preventDefault();
        $.ajax({
            type: "POST",
            url: "/api/link",
            data: $(this).serialize(),
            success: function (data, status, request) {
                $("#result").html(
                    "<div class='alert alert-success lead'><a target='_blank' href='"
                    + request.getResponseHeader('Location')
                    + "'>"
                    + request.getResponseHeader('Location')
                    + "</a></div>");
            },
            error: function (xhr, textStatus, errorThrown) {
                var errorMessage = xhr.responseText || "Error desconocido";

                $("#result").html(
                    "<div class='alert alert-danger lead'>" + errorMessage + "</div>");
            }
        });
    });
});
