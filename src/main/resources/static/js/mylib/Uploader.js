define(
    [ 'COS', "plupload" ],
    function( COS, uploader ){
        //初始化，从后台拿界面，配置，包括国际化配置、上传的平台配置
        //显示界面，绑定各种
        /**
         * container, 是上传组件的DOM对象
         * uploadBut, 是最后确认上传的按键，这个按键与上传选择文件是不同的按键
         * callback，是文件上传成功后的回调函数，用以处理上传文件后，再发送的其它参数
         * callback函数将接收到参数，
         *      1. 云存储或本地存储的内部文件名，innerId
         *      2. 用户上传文件名
         * 
         * 
         */
        function Uploader( 
            container,      //容器
            eventcenter,    //事件中心
            uploadBut,
            lang
        ){
            this.name = 'UPLOADER';
            this.container = container;
            //label用来区分不同的上传组件发送的上传成功信息，在同一个页面中有多个上传组件的时候，在上传组件的容器上要设置
            this.fireLabel = !!this.container.attr('label') ? this.container.attr('label') : 'uploaded';
            this.ec = eventcenter;
            this.ec.addHandler( this );

            this.uploaderHandler = null;

            this.upInterface = null;
            this.upComponent = null;

            this.uploadBut = uploadBut;

            this.currentFileName = null;    //上传原始文件名

            this.exports = [];
            this.eventHandlers = [];

            var _that = this;
            //从后台取上传组件HTML，HTML中有国际化的属性，有一个platform属性，指定上传的方式
            this.init = function(){
                $.post(
                    '/component/uploader/' + lang,
                    function( rst ){
                        
                        var dom = $(rst);
                        var platform = dom.attr('platform');
                        
                        _that.container.empty().append( rst );
                        switch( platform ){
                            case 'COS':
                                _that.upInterface =  CosUploader;                                
                                break;
                            case 'OSS':
                                _that.upInterface =  OssUploader;     
                                break;
                            case 'LOCAL':
                                _that.upInterface =  LocalUploader;     
                                break;
                        }
                        _that.initPluploader();

                        if(!!_that.uploadBut){
                            _that.uploadBut.click(  //确定上传按键绑定点击事件
                                function(){
                                    _that.upInterface.upload();
                                }
                            );
                        }
                        return _that;
                    }
                )
            }
            
            var CosUploader = {
                type: 'COS',
                upload : function(){
                   $.get(
                        '/file/uploader/cos/sign', 
                        function( data ){
                            var cosClient = new COS({
                                SecretId: data.credentials.tmpSecretId,
                                SecretKey: data.credentials.tmpSecretKey,
                                XCosSecurityToken: data.credentials.sessionToken
                            });
                            _that.innerId = data.requestId;
                            cosClient.putObject({
                                Bucket: data.bucket,
                                Region: data.region,
                                Key: data.requestId, 
                                StorageClass: 'STANDARD',
                                Body: _that.currentFile.getNative(),    //从PLUPLOAD返回原始的WINDOW FILE对象
                                onProgress: function (progressData) {
                                    _that.container.find('.pre').css( 'width',  progressData.percent *100 + '%');
                                }
                            }, function (err, data) {
                                if( !!err ){
                                    _that.ec.fire( 
                                        _that.name,
                                        _that.fireLabel,                                    
                                        null    //回传的参数为null, 表示上传失败
                                    ); 
                                    return;
                                }
                                _that.ec.fire( 
                                    _that.name,
                                    _that.fireLabel,                                       
                                    {
                                        type: _that.upInterface.type,
                                        originName : _that.currentFileName,
                                        innerId:  _that.innerId //设置内部文件名, COS上传回调的处理
                                    }
                                ); 
                                _that.currentFileName = null;       //清除暂存变量
                                _that.innerId = null;
                                //_that.container.find('.note').text('');
                                //_that.container.find('.pre').css( 'width',  '0px');
                                 //复位plupload
                                
                            });
                        }
                    );
                }
            };

            var OssUploader = {
                type: 'OSS',
                upload : function(){
                    $.post(
                        '/file/uploader/oos/sign',
                        function( rst ){
                            _that.innerId = rst.dir; //设置内部文件名
                            _that.upComponent.setOption({
                                'url': rst.host,
                                'multipart_params': {
                                    'key' : rst.dir,
                                    'policy': rst.policy,
                                    'OSSAccessKeyId': rst.accessid,
                                    'success_action_status' : '200', //让服务端返回200,不然，默认会返回204
                                    'callback' : rst.callback ,
                                    'signature': rst.signature,
                                }
                            });
                            _that.upComponent.start();
                        }
                    )
                }
            };

            var LocalUploader = {
                type: 'LOCAL',
                upload : function(){
                    $.post(
                        '/file/uploader/local/sign',
                        function( rst ){
                            _that.innerId = rst.uuid;
                            _that.upComponent.setOption({
                                'url': '/file/uploader/local/upload',
                                'multipart_params': {
                                    'uuid' : rst.uuid
                                }
                            });
                            _that.upComponent.start();

                        }
                    )
                }
            };

            //初始化plupload,绑定事件
            //事件1. 选择文件
            //事件2. 上传
            //上传前要把前台可能的表单事件一并传上去，再去拿签名
            //拿到签名，实现文件上传

            this.initPluploader = function(){
                this.upComponent = new uploader.Uploader(uploaderParams);
                this.upComponent.init();
            }


            var uploaderParams = {
                browse_button : 'selectbut', 
	            container: 'uploadDiv',
	            flash_swf_url : '../third-party/plupload-2.1.2/js/Moxie.swf',
	            silverlight_xap_url : '../third-party/plupload-2.1.2/js/Moxie.xap',
                url : '/file/uploader/local/sign',
                http_method: "POST",
                multipart: true,    
                filters: {
                    mime_types : [ 
                        { title : "Image files", extensions : "jpg,gif,png,bmp" }, 
                        { title : "Zip files", extensions : "zip,rar" },
                        { title : "office files", extensions : "docx,doc,ppt,xls,xlsx" },
                        { title : "pdf files", extensions : "pdf" }
                    ],
                    max_file_size : '100mb', //最大只能上传10mb的文件
                    prevent_duplicates : true //不允许选取重复文件
                },
                init: {
                    PostInit: function() {},
            
                    FilesAdded: function(up, files) {
                        container.find('.note').text( files[0].name );
                        container.find('.pre').css( 'width',  '0px');
                        _that.currentFileName = files[0].name ;
                        _that.currentFile = files[0];
                    },
            
                    BeforeUpload: function(up, file) {},
            
                    UploadProgress: function(up, file) {
                        container.find('.pre').css( 'width',  file.percent + '%');
                    },
            
                    FileUploaded: function(up, file, info) { //OSS, LOCAL二种上传最终的回调处理

                        _that.ec.fire( 
                            _that.name,
                            _that.fireLabel,                                  
                            {
                                type: _that.upInterface.type,
                                originName : _that.currentFileName,
                                innerId: _that.innerId
                            }
                        ); 
                        
                        _that.currentFileName = null;
                        _that.innerId = null;
                        _that.upComponent.refresh();        //复位
                        //_that.container.find('.note').text('');
                        //_that.container.find('.pre').css( 'width',  '0px');
                    },
            
                    Error: function(up, err) {         //出错处理
                        _that.ec.fire( 
                            _that.name,
                            _that.fireLabel,                                      
                            null    //回传为空，表示失败
                        ); 
                    }
                }
            }

        };
        
        return Uploader; 

    }
)