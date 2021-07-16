/***
 * @desc 扩展wangEditor的菜单栏功能
 * @remark 为方便后端维护编辑器，本次新增的编辑器菜单就在现有的代码基础上改，若您有时间维护编辑器，建议重构编辑器。
 * @author jrzhouweixue
 * @time 2018-4-25
 */

// 获取 wangEditor 构造函数和 jquery

var wangEditorBtnPlug = {
  /**
   * 扩展编辑器菜单栏，添加特定的a标签。
   */
  templateBtn: function() {
    var E = window.wangEditor;

    E.createMenu(function(check) {
      var paramsObj = {
        class: 'robot-btn  robot-line-btn', //定义按钮标签上要添加的线型class
        planeClass: 'robot-btn  robot-plane-btn robot-btn-colorWhite', //定义按钮标签上要添加的线型class
        skipClass: 'bridgeApp', //定义是否跳转原生要添加的class
        data: {
          //定义a标签上要添加的data，例如：data-sessionid="__data-sessionid__"则配置如下
          'data-sessionid': '',
          'data-requestno': '',
          'data-messageid': '',
        },
      };
      var dataStr = '';
      $.each(paramsObj.data, function(key, value) {
        var temp = key + '="' + value + '" ';
        dataStr += temp;
      });

      var menuId = 'templateBtn';
      if (!check(menuId)) {
        return;
      }
      var editor = this;

      // 创建 menu 对象
      var menu = new E.Menu({
        editor: editor,
        id: menuId,
        title: '按钮',
      });

      // 创建 dropPanel
      var $content = $('<div></div>');
      var $btntypeDiv = $(
        '<div class="clearfix">' +
          '<label>按钮类型：</label>\n' +
          '<input type="radio" class="block" name="btn-type"  value="line" checked>线型按钮\n' +
          '<input type="radio" class="block" name="btn-type" value="plane">面型按钮</div>'
      );
      var $div1 = $('<div style="margin:20px 10px;" class="clearfix"></div>');
      var $div2 = $div1.clone();
      var $div3 = $div1.clone();
      var $div4 = $('<div style="margin:0px 10px;" class="clearfix"></div>');
      var $div5 = $div1.clone().css('margin', '0 10px');
      var $textInput = $('<input type="text" class="block" placeholder="文本:不超过8个字符"/>');
      var $urlInput = $('<input type="text" class="block" placeholder="链接"/>');
      var $btnSkip = $('<input type="checkbox" class="block">' + '<label>是否跳转原生</label>');
      var $dataInput = $('<input type="text" class="block" disabled="disabled"/>');
      var $btnSubmit = $('<button class="right">提交</button>');
      var $btnCancel = $('<button class="right gray">取消</button>');

      $div1.append($textInput);
      $div2.append($urlInput);
      $div3.append($btnSkip);
      $div4.append($dataInput);
      $div5.append($btnSubmit).append($btnCancel);
      $content
        .append($btntypeDiv)
        .append($div1)
        .append($div2)
        .append($div3)
        .append($div4)
        .append($div5);

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

        //初始化复选框
        $btnSkip.attr('checked', false);
        // 重置 input
        $textInput.val('');
        $urlInput.val('https://');
        $dataInput.attr({ disabled: 'disabled' }).val('');

        // 获取url
        var url = '';
        var rangeElem = editor.getRangeElem();
        rangeElem = editor.getSelfOrParentByName(rangeElem, 'a');
        if (rangeElem) {
          url = rangeElem.href || '';
          var type = $(rangeElem).data('type');
          $($btntypeDiv)
            .find("input:radio[value='" + type + "']")
            .attr('checked', 'true');
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

        // 获取 skipData
        //var skipData = $(rangeElem).data('val') || '';
        var skipData = $(rangeElem).attr('data-val') || '';
        if (skipData) {
          $btnSkip.attr('checked', true);
          $dataInput.removeAttr('disabled');
        }

        // 设置 url 、text和skipData
        url && $urlInput.val(url);
        text && $textInput.val(text);
        skipData && $dataInput.val(skipData);

        // 如果有选区内容，textinput 不能修改 跳转原生按钮恢复成未选中状态，dataInput值清空
        if (!isRangeEmpty) {
          $textInput.attr('disabled', true);
          $btnSkip.attr('checked', false);
          $dataInput.attr({ disabled: 'disabled' }).val('');
        } else {
          $textInput.removeAttr('disabled');
        }

        // 显示（要设置好了所有input的值和属性之后再显示）
        dropPanel.show();
      };

      $btnSkip.click(function() {
        // 判断是否选择跳转，是否禁用输入框
        if ($btnSkip.is(':checked')) {
          $dataInput.removeAttr('disabled').focus();
        } else {
          $dataInput.attr({ disabled: 'disabled' }).val('');
        }
      });

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
        var skipData = $.trim($dataInput.val());
        var btnTypeValue = $('input[name="btn-type"]:checked').val();
        if (!url || (url.indexOf('http://') != 0 && url.indexOf('https:') != 0)) {
          //menu.dropPanel.focusFirstInput();
          alert("地址输入不合法，请输入以'http://'或'https://'开头的地址信息");
          $urlInput.focus();
          return;
        }
        if (text.length > 8) {
          alert('按钮文本不能超过8个字符');
          menu.dropPanel.focusFirstInput();
          return;
        }
        if (!text) {
          text = url;
        }

        if ($btnSkip.is(':checked')) {
          if (!skipData) {
            alert('请输入原生链接');
            $dataInput.focus();
            return;
          }
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
          if (btnTypeValue == 'line') {
            //线型按钮
            $newLinks.addClass(paramsObj.class); //增加class
            $newLinks.attr('data-type', 'line'); // 增加按钮属性
          } else {
            //面型按钮
            $newLinks.addClass(paramsObj.planeClass); //增加class
            $newLinks.attr('data-type', 'plane'); // 增加按钮属性
          }
          if ($btnSkip.is(':checked')) {
            //跳转按钮
            $newLinks.addClass(paramsObj.skipClass); // 若选择跳转，则增加class
            $newLinks.attr('data-val', skipData); // 增加data-val属性
          }

          //$newLinks.addClass(paramsObj.class);    //增加class
          $newLinks.attr(paramsObj.data); //增加data
          // 去掉之前做的标记
          $oldLinks.removeAttr(uniqId);
        } else if (targetElem) {
          // 无选中区域，在 a 标签之内，修改该 a 标签的内容和链接
          $linkElem = $(targetElem);
          commandFn = function() {
            $linkElem.attr('href', url);
            $linkElem.text(text);
            if (btnTypeValue == 'line') {
              //线型按钮
              $linkElem.addClass(paramsObj.class).removeClass('robot-plane-btn'); //增加class
              $linkElem.data('type', 'line'); // 增加按钮属性
            } else {
              //面型按钮
              $linkElem.addClass(paramsObj.planeClass).removeClass('robot-line-btn'); //增加class
              $linkElem.data('type', 'plane'); // 增加按钮属性
            }
            if ($btnSkip.is(':checked')) {
              //跳转按钮
              $linkElem.addClass(paramsObj.skipClass); // 若选择跳转，则增加class
              $linkElem.attr('data-val', skipData); // 增加data-val属性
            } else {
              $linkElem.removeClass(paramsObj.skipClass);
              $linkElem.removeAttr('data-val');
            }
          };
          callback = function() {
            var editor = this;
            editor.restoreSelectionByElem(targetElem);
          };
          // 执行命令
          editor.customCommand(e, commandFn, callback);
        } else {
          // 无选中区域，不在 a 标签之内，插入新的链接
          if (btnTypeValue == 'line') {
            //线型按钮
            if ($btnSkip.is(':checked')) {
              //跳转按钮
              linkHtml =
                '<a class="' +
                paramsObj.class +
                ' ' +
                paramsObj.skipClass +
                '" data-val=' +
                skipData +
                '  data-type="line" href="' +
                url +
                '" target="_blank">' +
                text +
                '</a>';
            } else {
              linkHtml =
                '<a class="' +
                paramsObj.class +
                '"  data-type="line" href="' +
                url +
                '" target="_blank">' +
                text +
                '</a>';
            }
          } else {
            //面型按钮
            if ($btnSkip.is(':checked')) {
              //跳转按钮
              linkHtml =
                '<a class="' +
                paramsObj.planeClass +
                ' ' +
                paramsObj.skipClass +
                '" data-val=' +
                skipData +
                '  data-type="plane" href="' +
                url +
                '" target="_blank">' +
                text +
                '</a>';
            } else {
              linkHtml =
                '<a class="' +
                paramsObj.planeClass +
                '"  data-type="plane"  href="' +
                url +
                '" target="_blank">' +
                text +
                '</a>';
            }
          }
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

module.exports = wangEditorBtnPlug;
