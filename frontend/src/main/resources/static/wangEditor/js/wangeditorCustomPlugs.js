/***
 * @desc 扩展wangEditor的菜单栏功能
 * @remark 为方便后端维护编辑器，本次新增的编辑器菜单就在现有的代码基础上改，若您有时间维护编辑器，建议重构编辑器。
 * @author jrranyingxia
 * @time 2017-11-22
 */

// 获取 wangEditor 构造函数和 jquery

var wangeditorCustomPlugs = {
  /**
   * 扩展编辑器菜单栏，添加特定的a标签。
   */
  templateLinkA: function() {
    var E = window.wangEditor;

    E.createMenu(function(check) {
      var paramsObj = {
        class: 'bot-to-service', //定义a标签上要添加的class
        data: {
          //定义a标签上要添加的data，例如：data-sessionid="__data-sessionid__"则配置如下
          'data-sessionid': '__data-sessionid__',
          'data-requestno': '__data-requestno__',
          'data-messageid': '_data-messageid_',
        },
      };
      var dataStr = '';
      $.each(paramsObj.data, function(key, value) {
        var temp = key + '="' + value + '" ';
        dataStr += temp;
      });

      var menuId = 'templateLinkA';
      if (!check(menuId)) {
        return;
      }
      var editor = this;

      // 创建 menu 对象
      var menu = new E.Menu({
        editor: editor,
        id: menuId,
        title: 'a标签',
      });

      // 创建 dropPanel
      var $content = $('<div></div>');
      var $div1 = $('<div style="margin:20px 10px;" class="clearfix"></div>');
      var $div2 = $div1.clone();
      var $div3 = $div1.clone().css('margin', '0 10px');
      var $textInput = $('<input type="text" class="block" placeholder="文本"/>');
      var $urlInput = $('<input type="text" class="block" placeholder="链接"/>');
      var $btnSubmit = $('<button class="right">提交</button>');
      var $btnCancel = $('<button class="right gray">取消</button>');

      $div1.append($textInput);
      $div2.append($urlInput);
      $div3.append($btnSubmit).append($btnCancel);
      $content
        .append($div1)
        .append($div2)
        .append($div3);

      menu.dropPanel = new E.DropPanel(editor, menu, {
        $content: $content,
        width: 300,
      });

      // 定义click事件
      menu.clickEvent = function(e) {
        var menu = this;
        var dropPanel = menu.dropPanel;

        // -------------隐藏----------------
        if (dropPanel.isShowing) {
          dropPanel.hide();
          return;
        }

        // -------------显示----------------

        // 重置 input
        $textInput.val('');
        $urlInput.val('https://');

        // 获取url
        var url = '';
        var rangeElem = editor.getRangeElem();
        rangeElem = editor.getSelfOrParentByName(rangeElem, 'a');
        if (rangeElem) {
          url = rangeElem.href || '';
        }

        // 获取 text
        var text = '';
        var isRangeEmpty = editor.isRangeEmpty();
        if (!isRangeEmpty) {
          // 选区不是空
          text = editor.getRangeText() || '';
        } else if (rangeElem) {
          // 如果选区空，并且在 a 标签之内
          text = rangeElem.textContent || rangeElem.innerHTML;
        }

        // 设置 url 和 text
        url && $urlInput.val(url);
        text && $textInput.val(text);

        // 如果有选区内容，textinput 不能修改
        if (!isRangeEmpty) {
          $textInput.attr('disabled', true);
        } else {
          $textInput.removeAttr('disabled');
        }

        // 显示（要设置好了所有input的值和属性之后再显示）
        dropPanel.show();
      };

      // 定义 update selected 事件
      menu.updateSelectedEvent = function() {
        var rangeElem = editor.getRangeElem();
        rangeElem = editor.getSelfOrParentByName(rangeElem, 'a');
        if (rangeElem) {
          return true;
        }
        return false;
      };

      // 『取消』 按钮
      $btnCancel.click(function(e) {
        e.preventDefault();
        menu.dropPanel.hide();
      });

      // 『确定』按钮
      $btnSubmit.click(function(e) {
        e.preventDefault();
        var rangeElem = editor.getRangeElem();
        var targetElem = editor.getSelfOrParentByName(rangeElem, 'a');
        var isRangeEmpty = editor.isRangeEmpty();

        var $linkElem, linkHtml;
        var commandFn, callback;
        var $txt = editor.txt.$txt;
        var $oldLinks, $newLinks;
        var uniqId = 'link' + E.random();

        // 获取数据
        var url = $.trim($urlInput.val());
        var text = $.trim($textInput.val());

        if (!url || (url.indexOf('http://') != 0 && url.indexOf('https:') != 0)) {
          //menu.dropPanel.focusFirstInput();
          alert("地址输入不合法，请输入以'http://'或'https://'开头的地址信息");
          $urlInput.focus();
          return;
        }
        if (!text) {
          text = url;
        }
        if (!isRangeEmpty) {
          // 选中区域有内容，则执行默认命令

          // 获取目前 txt 内所有链接，并为当前链接做一个标记
          $oldLinks = $txt.find('a');
          $oldLinks.attr(uniqId, '1');

          // 执行命令
          editor.command(e, 'createLink', url);

          // 去的没有标记的链接，即刚刚插入的链接
          $newLinks = $txt.find('a').not('[' + uniqId + ']');
          $newLinks.attr('target', '_blank'); // 增加 _blank
          $newLinks.addClass(paramsObj.class); //增加class
          $newLinks.attr(paramsObj.data); //增加data

          // 去掉之前做的标记
          $oldLinks.removeAttr(uniqId);
        } else if (targetElem) {
          // 无选中区域，在 a 标签之内，修改该 a 标签的内容和链接
          $linkElem = $(targetElem);
          commandFn = function() {
            $linkElem.attr('href', url);
            $linkElem.text(text);
          };
          callback = function() {
            var editor = this;
            editor.restoreSelectionByElem(targetElem);
          };
          // 执行命令
          editor.customCommand(e, commandFn, callback);
        } else {
          // 无选中区域，不在 a 标签之内，插入新的链接
          linkHtml =
            '<a class="' +
            paramsObj.class +
            '" href="' +
            url +
            '" target="_blank"' +
            dataStr +
            '>' +
            text +
            '</a>';
          if (E.userAgent.indexOf('Firefox') > 0) {
            linkHtml += '<span>&nbsp;</span>';
          }
          editor.command(e, 'insertHtml', linkHtml);
        }
      });

      // 增加到editor对象中
      editor.menus[menuId] = menu;
    });
  },
};

module.exports = wangeditorCustomPlugs;
