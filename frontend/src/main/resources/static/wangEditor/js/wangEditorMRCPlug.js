/***
 * @desc 扩展wangEditor的菜单栏功能
 * @remark 为方便后端维护编辑器，本次新增的编辑器菜单就在现有的代码基础上改，若您有时间维护编辑器，建议重构编辑器。
 * @author jrfengguangnan
 * @time 2018-7-23
 */

// 获取 wangEditor 构造函数和 jquery
var wangEditorMRCPlug = {
  /**
   * 扩展编辑器菜单栏，添加特定的a标签。
   */
  templateMRC: function() {
    var E = window.wangEditor;

    E.createMenu(function(check) {
      var paramsObj = {
        class: 'robot-card robot-icon',
        data: {
          'data-requestno': '',
        },
      };
      var dataStr = '';
      $.each(paramsObj.data, function(key, value) {
        var temp = key + '="' + value + '" ';
        dataStr += temp;
      });

      var menuId = 'templateMRC';
      if (!check(menuId)) {
        return;
      }
      var editor = this;

      // 创建 menu 对象
      var menu = new E.Menu({
        editor: editor,
        id: menuId,
        title: 'MRC搜索',
      });

      // 创建 dropPanel
      var $content = $('<div></div>');
      var $div1 = $('<div style="margin:20px 10px;" class="clearfix"></div>');
      var $div2 = $div1.clone();
      var $div3 = $div1.clone().css('margin', '0 10px');
      var $nameInput = $(
        '<input type="text" class="block" placeholder="文本" disabled="disabled"/>'
      );
      var $codeInput = $(
        '<input type="text" class="block" placeholder="Code" disabled="disabled"/>'
      );
      var $btnSubmit = $('<button class="right">提交</button>');
      var $btnCancel = $('<button class="right gray">取消</button>');

      $div1.append($nameInput);
      $div2.append($codeInput);
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
      menu.clickEvent = function() {
        var menu = this;
        var dropPanel = menu.dropPanel;

        // -------------隐藏----------------
        if (dropPanel.isShowing) {
          dropPanel.hide();
          return;
        }

        // -------------显示----------------
        // 初始化 input
        $nameInput.val(arguments[1]);
        $codeInput.val(arguments[2]);
        dropPanel.show();
      };

      // 定义 update selected 事件
      menu.updateSelectedEvent = function() {
        var rangeElem = editor.getRangeElem();
        rangeElem = editor.getSelfOrParentByName(rangeElem, 'div');
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
        // 获取数据
        var codes = $.trim($codeInput.val());
        var names = $.trim($nameInput.val());

        if (!names) {
          menu.dropPanel.focusFirstInput();
          return;
        }
        if (!codes) {
          return;
        }

        var linkHtml =
          '<input class="' +
          paramsObj.class +
          '" date-component-code="' +
          codes +
          '" value="' +
          names +
          '" />';
        if (E.userAgent.indexOf('Firefox') > 0) {
          linkHtml += '<span>&nbsp;</span>';
        }
        editor.command(e, 'insertHtml', linkHtml);

        var currentTarget = null;
        $('.robot-card').bind('focus', function() {
          currentTarget = $(this);
        });
        $('.robot-card').bind('blur', function() {
          currentTarget = null;
        });

        //按下backspace，删除整个节点
        var $txt = editor.txt.$txt;
        $txt.on('keydown', function(e) {
          if (currentTarget != null) {
            if (e.keyCode == 8) {
              //keycode为8表示退格键
              //删除整个节点
              currentTarget.remove();
              // 阻止冒泡
              e.stopPropagation();
            }
          }
        });
      });

      // 增加到editor对象中
      editor.menus[menuId] = menu;

      //menu存成全局变量
      window.componentMenu = menu;
    });
  },
};

module.exports = wangEditorMRCPlug;
