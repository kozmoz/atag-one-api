$(document).ready(function () {

    // Toggle visibility of install insrtuction Debian.
    $('#java-install-debian-link').on('click', function () {
        $('#java-install-debian').toggleClass('hidden');
        $('#java-install-raspbian').addClass('hidden');
    });

    // Toggle visibility of install instructions Raspberry Pi.
    $('#java-install-raspbian-link').on('click', function () {
        $('#java-install-raspbian').toggleClass('hidden');
        $('#java-install-debian').addClass('hidden');
    });

    function pad(n) {
        return (n < 10) ? ("0" + n) : n;
    }

    // Display and update current timestamp in example message.
    function updateTimestamp() {
        var now = new Date();
        var timestamp = now.getFullYear() + '-' + (now.getMonth() + 1) + '-' + now.getDate() + ' ' + pad(now.getHours()) + ':' + pad(now.getMinutes()) + ':' + pad(now.getSeconds());
        $('.bh-now').text(timestamp);
        setTimeout(updateTimestamp, 1000);
    }

    updateTimestamp();
});


