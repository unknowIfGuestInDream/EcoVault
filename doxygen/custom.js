/* EcoVault Doxygen 自定义 JavaScript */

// 页面加载完成后执行
$(document).ready(function() {
    // 为外部链接添加图标
    $("a[href^='http']").each(function() {
        if (this.hostname && this.hostname !== window.location.hostname) {
            $(this).attr('target', '_blank');
            $(this).attr('rel', 'noopener noreferrer');
            $(this).append(' <i class="fa fa-external-link"></i>');
        }
    });

    // 为代码块添加复制按钮
    $('.fragment').each(function() {
        var $codeBlock = $(this);
        var $copyButton = $('<button class="copy-button" title="复制代码">复制</button>');

        $copyButton.on('click', function() {
            var code = $codeBlock.text();
            navigator.clipboard.writeText(code).then(function() {
                $copyButton.text('已复制!');
                setTimeout(function() {
                    $copyButton.text('复制');
                }, 2000);
            }, function(err) {
                console.error('复制失败:', err);
            });
        });

        $codeBlock.css('position', 'relative');
        $copyButton.css({
            'position': 'absolute',
            'top': '5px',
            'right': '5px',
            'padding': '5px 10px',
            'background-color': '#007bff',
            'color': 'white',
            'border': 'none',
            'border-radius': '3px',
            'cursor': 'pointer',
            'font-size': '12px'
        });

        $codeBlock.prepend($copyButton);
    });

    // 平滑滚动
    $('a[href^="#"]').on('click', function(e) {
        var target = this.hash;
        var $target = $(target);

        if ($target.length) {
            e.preventDefault();
            $('html, body').animate({
                'scrollTop': $target.offset().top - 20
            }, 500);
        }
    });

    // 为表格添加排序功能
    if (typeof $.fn.tablesorter === 'function') {
        $('table.doxtable').tablesorter();
    }

    // 回到顶部按钮
    var $backToTop = $('<button id="back-to-top" title="返回顶部">↑</button>');
    $backToTop.css({
        'position': 'fixed',
        'bottom': '30px',
        'right': '30px',
        'width': '50px',
        'height': '50px',
        'background-color': '#007bff',
        'color': 'white',
        'border': 'none',
        'border-radius': '50%',
        'font-size': '24px',
        'cursor': 'pointer',
        'display': 'none',
        'box-shadow': '0 2px 5px rgba(0,0,0,0.3)',
        'z-index': '1000'
    });

    $('body').append($backToTop);

    $(window).scroll(function() {
        if ($(this).scrollTop() > 300) {
            $backToTop.fadeIn();
        } else {
            $backToTop.fadeOut();
        }
    });

    $backToTop.on('click', function() {
        $('html, body').animate({scrollTop: 0}, 500);
        return false;
    });

    // 图片灯箱效果
    $('div.contents img').each(function() {
        var $img = $(this);
        $img.css('cursor', 'pointer');
        $img.on('click', function() {
            var $lightbox = $('<div class="lightbox"></div>');
            var $lightboxImg = $('<img>').attr('src', $img.attr('src'));
            var $closeBtn = $('<span class="close-lightbox">&times;</span>');

            $lightbox.css({
                'position': 'fixed',
                'top': '0',
                'left': '0',
                'width': '100%',
                'height': '100%',
                'background-color': 'rgba(0,0,0,0.9)',
                'display': 'flex',
                'justify-content': 'center',
                'align-items': 'center',
                'z-index': '9999'
            });

            $lightboxImg.css({
                'max-width': '90%',
                'max-height': '90%'
            });

            $closeBtn.css({
                'position': 'absolute',
                'top': '20px',
                'right': '40px',
                'color': 'white',
                'font-size': '40px',
                'cursor': 'pointer'
            });

            $lightbox.append($closeBtn);
            $lightbox.append($lightboxImg);
            $('body').append($lightbox);

            $lightbox.on('click', function() {
                $(this).remove();
            });
        });
    });

    // 控制台输出欢迎信息
    console.log('%c欢迎查看 EcoVault 技术文档！', 'color: #007bff; font-size: 20px; font-weight: bold;');
    console.log('%c项目地址: https://github.com/unknowIfGuestInDream/EcoVault', 'color: #6c757d; font-size: 14px;');
});
