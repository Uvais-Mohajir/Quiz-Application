(function () {
    const body = document.body;
    if (!body || body.dataset.disableIdleLogout === "true") {
        return;
    }

    const logoutForm = document.querySelector("form[action='/logout'], form[action$='/logout']");
    if (!logoutForm) {
        return;
    }

    const idleLimitMs = 3 * 60 * 1000;
    let timerId = null;

    function doLogout() {
        logoutForm.submit();
    }

    function resetTimer() {
        if (timerId) {
            clearTimeout(timerId);
        }
        timerId = setTimeout(doLogout, idleLimitMs);
    }

    [
        "click",
        "mousemove",
        "mousedown",
        "keydown",
        "scroll",
        "touchstart"
    ].forEach((eventName) => {
        document.addEventListener(eventName, resetTimer, { passive: true });
    });

    resetTimer();
})();
