(function () {
    if (window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
        return;
    }
    var interval = 3500;
    document.addEventListener('DOMContentLoaded', function () {
        document.querySelectorAll('.card-image[data-rotate]').forEach(function (el) {
            var imgs = el.querySelectorAll('img');
            if (imgs.length < 2) return;
            var i = 0;
            setInterval(function () {
                imgs[i].classList.remove('card-image-active');
                i = (i + 1) % imgs.length;
                imgs[i].classList.add('card-image-active');
            }, interval);
        });
    });
}());
