/* Closes an open nav group when the user clicks away or presses Escape.
   A bare <details> only toggles from its own summary, which leaves a dropdown
   stuck open until you click it again. Enhancement only — the menu works
   without this script, it just needs the second click. */
(function () {
    function closeAll(except) {
        document.querySelectorAll('details.nav-menu[open]').forEach(function (menu) {
            if (menu !== except) menu.removeAttribute('open');
        });
    }

    document.addEventListener('click', function (event) {
        var inside = event.target.closest ? event.target.closest('details.nav-menu') : null;
        closeAll(inside);
    });

    document.addEventListener('keydown', function (event) {
        if (event.key !== 'Escape') return;
        var open = document.querySelector('details.nav-menu[open]');
        if (!open) return;
        open.removeAttribute('open');
        var summary = open.querySelector('summary');
        if (summary) summary.focus();
    });
}());
