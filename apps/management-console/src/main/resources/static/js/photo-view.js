/* Opens a gallery photo over the page. Each a[data-photo-view] links a tile to its
   stored photo (at most 1024 px); a click shows that photo in one shared <dialog>
   instead of navigating. Without JavaScript the link opens the photo URL.
   The dialog closes on Escape (native), a click on the backdrop, or the button. */
(function () {
    var dialog = null;
    var image = null;

    function photoView() {
        if (dialog) return dialog;
        dialog = document.createElement('dialog');
        dialog.className = 'photo-view';
        image = document.createElement('img');
        var close = document.createElement('button');
        close.type = 'button';
        close.setAttribute('aria-label', 'Close');
        close.textContent = '×';
        close.addEventListener('click', function () { dialog.close(); });
        dialog.addEventListener('click', function (event) {
            if (event.target === dialog) dialog.close();
        });
        dialog.addEventListener('close', function () { image.removeAttribute('src'); });
        dialog.appendChild(close);
        dialog.appendChild(image);
        document.body.appendChild(dialog);
        return dialog;
    }

    document.addEventListener('click', function (event) {
        var link = event.target.closest ? event.target.closest('a[data-photo-view]') : null;
        if (!link || event.defaultPrevented || event.button !== 0
                || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) {
            return;
        }
        var view = photoView();
        if (typeof view.showModal !== 'function') return;
        event.preventDefault();
        var tile = link.querySelector('img');
        image.src = link.href;
        image.alt = tile ? tile.alt : '';
        view.showModal();
    });
}());
