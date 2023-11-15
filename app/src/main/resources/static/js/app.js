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
                var errorResponse = xhr.responseJSON; // Intenta obtener un mensaje de error personalizado del servidor
                var errorMessage = (errorResponse && errorResponse.message) ? errorResponse.message : "Error desconocido";

                $("#result").html(
                    "<div class='alert alert-danger lead'>" + errorMessage + "</div>");
            }
        });
    });
});
