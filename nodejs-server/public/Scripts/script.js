$(document).ready(function () {

    var settings = {
        host: 'ws://'+window.location.hostname+':9000'
    };

    var clientHasCarRegistered = false;

    var connection = new WebSocket(settings.host);

    $("#c1").click(function () {
        registerDeviceOrientationListenerForGivenCar("C1");
    });

    $("#c2").click(function () {
        registerDeviceOrientationListenerForGivenCar("C2");
    });

    function registerDeviceOrientationListenerForGivenCar(givenCar) {
        if (!clientHasCarRegistered) {
            if (window.DeviceOrientationEvent) {
                document.getElementById("doEvent").innerHTML = "DeviceOrientation";

                // Listen for the deviceorientation event and handle the raw data
                window.addEventListener('deviceorientation', function (eventData) {
                    // beta is the front-to-back tilt in degrees, where front is positive
                    var tiltFB = eventData.beta;

                    var translatedSpeed = translateSpeed(Math.round(tiltFB));

                    document.getElementById("doTiltFB").innerHTML = translatedSpeed;
                    // Listen for the deviceorientation event and handle the raw data
                    connection.send(givenCar + ": " + translatedSpeed);
                }, false);
                disableCarSelection();
            } else {
                document.getElementById("doEvent").innerHTML = "Not supported."
            }
        }
    }

    function translateSpeed(tiltFB) {
        /* we don't want to use the whole 360° of the device. We take the 90° from a device which
         has it's display face your body (0° = 0 speed) and you move the display up to 90° away
         from you until your device's display points to the sky (90° = 255 speed). */
        if (tiltFB >= 90) {
            tiltFB = 90;
        } else if (tiltFB <= 0) {
            tiltFB = 0;
        }
        /* the phone in vertical position equals 90°. As we think of this position as 0 speed we translate it.
         The other way around: a phone in horizontal position equals 0° while we think of this as "go ham (full speed)" */
        var reverseFB = 90 - tiltFB;
        var degreeAsPercentage = reverseFB / (0.9);
        return Math.round(255 * (degreeAsPercentage / 100));
    }

    function disableCarSelection() {
        $("#c1").attr("disabled", "disabled");
        $("#c2").attr("disabled", "disabled");
    }
});



