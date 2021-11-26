/**
 * 
 */
require.config({　
  baseUrl: '/js/third-party',
  paths: {　　　　　　
	  //----third-party
    "jquery": "jquery",
    "md5" : 'md5-js',
	  "plupload": 'plupload-2.1.2/js/plupload.full.min',
    "COS": 'cos-js-sdk-v5.min',
    "template": "template",
    "layui": "layui/layui.all",
    "paginate": "pagization",
    "store": "store.everything.min",
    
    //----mylib
    "eventcenter": "../mylib/EventCenter",
    "datacenter": "../mylib/DataCenter",
    "dataverifier": "../mylib/DataVerifier",
    "uploader": "../mylib/Uploader",
    "myLayer" : "../mylib/MyLayer",
    "widget" : "../mylib/Widget",
    "popup" : "../mylib/Popup",
    "editor" : "../mylib/Editor",
    "msgbox" : "../mylib/MessageBox",
    "confirm" : "../mylib/Confirm",
    "mailtpl" : "../mylib/MailTpl",
    "mystripe" : "../mylib/MyStripe",
    //test
    "test" : "../page/test/Test",
    "login" : "../page/login/Login",
    "resetpassword" : "../page/login/ResetPassword",
    "regist" : "../page/login/Regist",
    "active": "../page/login/Active",
   
    "home" : "../page/home/Home",
    "homecommon" : "../page/home/HomeCommon",
    "homemenu" : "../page/home/HomeMenu",

    "createpublisher" : "../page/publisher/Create",
    "listpublisher" : "../page/publisher/List",

    "listjournal" : "../page/journal/List",
    "configjournal" : "../page/journal/Config",
    "configds" : "../page/journal/ConfigDS",
    "team" : "../page/journal/TeamRole",
    "section" : "../page/journal/Section",
    "email" : "../page/journal/Email",
    "submitreview" : "../page/journal/SubmitReview",
    "workflow": "../page/journal/WorkFlow",
    "payment": "../page/journal/Payment",

    "usermanage": "../page/user/UserManagement",
    "profile": "../page/user/Profile",
    
    "message": "../page/message/Message",

    "submit" : "../page/submit/Submit",
    "submitdc" : "../page/submit/SubmitDc",
    "submitchecker" : "../page/submit/SubmitChecker",
    
    "listadapt": "../page/article/list/ListAdapt",
    "listquery" : "../page/article/list/Query",
    "listshowconfig" : "../page/article/list/ShowConfig",
    "render" : "../page/article/list/Render",

    "listboard": "../page/article/ArticleListBoard",
    "queryboard": "../page/article/ArticleListQueryBoard",

    "paperlist": "../page/article/list",
    "listtable" : "../page/article/ListTable",
    "article" : '../page/article/Article',
    "history": '../page/article/History',
    "submittab": '../page/article/SubmitTab',
    "reviewtab": '../page/article/reviewtab/ReviewTab',
    "reviewer": '../page/article/reviewtab/Reviewer', //审稿人邀请组织
    "decision": '../page/article/reviewtab/Decision', //审稿人邀请组织
    "fileuploader": '../page/article/FileUploader', //负责处理卡片页中文件上传
    "filedownloader": '../page/article/FileDownloader',
    

    //review
    "review" : '../page/article/Review', //审稿界面

    //
    "similarchecktab" : '../page/article/SimilarCheckTab', //审稿界面"SimilarCheckTab" 
    "paymenttab": '../page/article/Payment',
    "paymentcompent": '../page/PaymentCompent',
    "paymentpage": '../page/article/PaymentPage',

    //copyedit
    "copyedittab": '../page/article/Copyedit',
  }, 
  shim: {
	 'layer': {
　　　　deps: ['jquery'],
　　　　exports: "layer"
　　},
    'plupload': {
      deps: ['jquery'],
      exports: 'plupload'
    },
    'COS': {
      deps: ['jquery'],
      exports: 'COS'
    }
  }
});

require(["jquery"], function ($) {
	
    var currentPage = $("#current-page").attr("current-page");
    var targetModule = $("#current-page").attr("target-module");
    switch( targetModule ){
    	case 'test':
    		require(["test"], function (test) {
    			  new test( $('#container') ).init();
    	  });
    		break;
      case 'login':
    		require(["login"], function (login) {
    			  new login( $('#login') ).init();
    	  });
    		break;
      case 'resetpassword':
    		require(["resetpassword"], function (resetP) {
    			  new resetP( $('#container') ).init();
    	  });
    		break;
      case 'active':
    		require(["active"], function (active) {
    			  new active( $('#container') ).init();
    	  });
    		break;
      case 'regist':
    		require(["regist"], function (regist) {
    			  new regist( $('#container') ).init();
    	  });
    		break;
      case 'home':
    		require(["home"], function (home) {
    			  new home( $('#frame') ).init();
    	  });
    		break;
      case 'create-publisher':
    		require(["createpublisher"], function (create) {
    			  new create( $('#frame') ).init();
    	  });
    		break;
      case 'list-publisher':
        require(["listpublisher"], function (list) {
            new list( $('#frame') ).init();
        });          
        break;
      case 'journal-list':
        require(["listjournal"], function (list) {
            new list( $('#frame') ).init();
        });          
        break;
      case 'journal-config':
        require(["configjournal"], function (cj) {
            new cj( $('#frame') ).init();
        });          
        break;
      case 'usermanage':
        require(["usermanage"], function (um) {
            new um( $('#frame') ).init();
        });          
        break;
      case 'profile':
        require(["profile"], function (profile) {
            new profile( $('#frame') ).init();
        });          
        break;
      case 'submit':
        require(["submit"], function (submit) {
            new submit( $('#frame') ).init();
        });          
        break;
      case 'message':
        require(["message"], function (message) {
            new message( $('#frame') ).init();
        });
        break;
      case 'paperlist':
        require(["paperlist"], function (paperlist) {
            new paperlist( $('#frame') ).init();
        });
        break;
      case 'article':
        require(["article"], function (article) {
            new article( $('#frame') ).init();
        });
        break;
      case 'review':
        require(["review"], function (re) {
            new re( $('#reviewpage') ).init();
        });
        break;
      case 'paymentpage':
        require(["paymentpage"], function (py) {
            new py( $('#paymentPage') ).init();
        });
        break;
      case 'paymentcompent':
        require(["paymentcompent"], function (py) {
            new py( $('#paymentPage') ).init();
        });
        break;
    }
    
    return;
});
 