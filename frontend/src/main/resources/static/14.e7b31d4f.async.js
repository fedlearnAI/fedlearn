(window.webpackJsonp=window.webpackJsonp||[]).push([[14],{"61Lz":function(e,t,n){"use strict";n("5NDa");var r=n("5rEg"),a=n("htGi"),s=n.n(a),o=n("/HRN"),i=n.n(o),l=n("WaGi"),u=n.n(l),c=n("ZDA2"),p=n.n(c),d=n("/+P4"),h=n.n(d),m=n("N9n2"),f=n.n(m),g=n("xHqa"),v=n.n(g),w=n("q1tI"),y=n.n(w),E=function(e){function t(){return i()(this,t),p()(this,h()(t).apply(this,arguments))}return f()(t,e),u()(t,[{key:"render",value:function(){var e=this;return y.a.createElement(r.a.TextArea,s()({},this.props,{onBlur:function(t){t.target.value=(t.target.value||"").trim(),e.props.onChange(t),e.props.onBlur(t)}}))}}]),t}(w.PureComponent);v()(E,"defaultProps",{onChange:function(){},onBlur:function(){}});var P=function(e){function t(){return i()(this,t),p()(this,h()(t).apply(this,arguments))}return f()(t,e),u()(t,[{key:"render",value:function(){var e=this;return y.a.createElement(r.a,s()({},this.props,{onBlur:function(t){t.target.value=(t.target.value||"").trim(),e.props.onChange(t),e.props.onBlur(t)}}))}}]),t}(w.PureComponent);v()(P,"defaultProps",{onChange:function(){},onBlur:function(){}}),P.TextArea=E,t.a=P},M7LS:function(e,t,n){"use strict";n.r(t),n.d(t,"default",function(){return k});n("+L6B");var r,a=n("2/Rp"),s=n("htGi"),o=n.n(s),i=(n("miYZ"),n("tsqr")),l=n("/HRN"),u=n.n(l),c=n("WaGi"),p=n.n(c),d=n("ZDA2"),h=n.n(d),m=n("/+P4"),f=n.n(m),g=n("K47E"),v=n.n(g),w=n("N9n2"),y=n.n(w),E=n("xHqa"),P=n.n(E),C=(n("y8nQ"),n("Vl3Y")),b=n("q1tI"),N=n.n(b),B=n("MuoO"),L=n("61Lz"),S=n("aCH8"),q=n.n(S),D=(n("KKVG"),C.a.Item),F={labelCol:{span:24},wrapperCol:{span:24}},k=Object(B.connect)()(r=C.a.create({})(r=function(e){function t(){var e,n;u()(this,t);for(var r=arguments.length,a=new Array(r),s=0;s<r;s++)a[s]=arguments[s];return n=h()(this,(e=f()(t)).call.apply(e,[this].concat(a))),P()(v()(n),"state",{showLogin:!0}),P()(v()(n),"handleSubmit",function(e){e&&e.preventDefault();var t=n.props,r=t.form.validateFields,a=t.dispatch;r(function(e,t){e||a({type:"user/login",payload:{username:t.username,password:q()(t.password)}}).then(function(e){})})}),P()(v()(n),"handleShowRegister",function(e){e&&e.preventDefault(),n.setState({showLogin:!1})}),P()(v()(n),"handlePwdChange",function(e){var t=n.props.form,r=t.getFieldValue,a=t.resetFields;e.target.value!=r("conPassword")&&a(["conPassword"])}),P()(v()(n),"handlePwdBlur",function(e){var t=n.props.form,r=t.getFieldValue,a=t.resetFields;e.target.value!=r("rePassword")&&(a(["conPassword"]),i.a.error("密码不一致，请重新输入"))}),P()(v()(n),"handleRegister",function(e){e&&e.preventDefault();var t=n.props,r=t.form.validateFields,a=t.dispatch;r(function(e,t){e||a({type:"user/register",payload:{username:t.reUsername,password:q()(t.rePassword)}}).then(function(e){e&&n.setState({showLogin:!0})})})}),n}return y()(t,e),p()(t,[{key:"render",value:function(){var e=this.props.form.getFieldDecorator,t=this.state.showLogin;return N.a.createElement("div",{className:"register"},t&&N.a.createElement("div",{className:"register-content"},N.a.createElement("h3",{className:"register-title"},"登 录"),N.a.createElement("p",{className:"register-sub-title"},"请输入以下信息完成登录"),N.a.createElement(C.a,{onSubmit:this.handleSubmit},N.a.createElement(D,o()({},F,{label:"账号"}),e("username",{rules:[{required:!0,message:"账号不能为空！"}]})(N.a.createElement(L.a,{placeholder:"请输入账号"}))),N.a.createElement(D,o()({},F,{label:"密码"}),e("password",{rules:[{required:!0,message:"密码不能为空！"}]})(N.a.createElement(L.a,{type:"password",placeholder:"请输入密码"}))),N.a.createElement(D,F,N.a.createElement(a.a,{type:"primary",style:{width:"100%"},onClick:this.handleSubmit},"登录")),N.a.createElement(D,F))))}}]),t}(b.PureComponent))||r)||r},t33a:function(e,t,n){e.exports=n("cHUP")(10)}}]);