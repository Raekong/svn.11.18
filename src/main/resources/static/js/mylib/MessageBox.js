define(
    ["editor", "template", "uploader", "widget"],
    function(editor, template, uploader, widget){
        function MessageBox( 
            lang, 
            container, 
            eventcenter,
            title, 
            content 
        ){
            this.name = "MESSAGEBOX";
            this.lang = lang;
            this.container = container;
            this.label = container.attr('eventLabel');
            this.label = !! this.label ? this.label : 'sendmsg';
            this.ec = eventcenter;
            this.ec.addHandler( this );

            this.title = title;
            this.content = content;
            this.wg = new widget();

            var _that = this;

            console.log(this.label);

            //sender,包含二个要素，3.ACCOUNT,5. SENDERNAME;
            //RECEIVER,包含三个要素，1. id(如果是本系统用户的话), 2. name, 3. email
            
            this.init = function(){
              
                _that.uploader = _that.initUploader('uploadDiv', _that.ec, 'uploadConfirm', true).init();
                _that.editor = _that.initEditor('editor-container', false, 208).init();

                if( !!_that.title ) {  _that.container.find('[data-title]').val(_that.title) }
                if( !!_that.content ) {  _that.editor.set(_that.content ) }
                        
                _that.container.find('#send').click(
                   function(){
                        var data = {}; 
                        data.title = _that.container.find('[data-title]').val();
                        data.content = _that.editor.html();
                        data.attachments = JSON.stringify(_that.attachments);
                        data.isEmail = _that.container.find('#withemail').hasClass('checked');
                        //console.log(data);
                        _that.ec.fire(
                            _that.name,
                            _that.label, //回传的事件名               
                            data    //回传的邮件参数
                        );
                    }
                );       
                
                _that.container.find('#withemail').click(
                    function(){
                        if( $(this).hasClass('checked')){
                            $(this).removeClass('checked');
                        }else
                            $(this).addClass('checked');
                    }
                );
            }

            this.initEditor = function( editorId, lang ){
                return new editor( editorId, lang);
            }

            this.initUploader = function(uploadId, ec, uploadButId, lang){
                var up = new uploader(
                    _that.container.find('#'+uploadId), 	//选择文件按键
                    ec,
                    container.find('#'+uploadButId),  //确定上传按键
                    lang
                );
                return up;
            }

            this.fileTpl = "{{each files as file }}<div class='file' innerId='{{file.innerId}}' originname='{{file.originName}}'>{{file.originName}} <span><i class='fa fa-close'></i></span></div>{{/each}}";
                
            this.attachments = [];
            this.removeFile = function( ){
                var innerId = $(this).closest('.file').attr('innerId');
                var index = 0;
                _that.attachments.forEach(
                    function(data){
                        if( data.innerId == innerId ){
                            return;
                        }else{
                            index++;
                        }
                    }
                )
                _that.attachments.splice(index, 1);
                var data = {};
                var render = template.compile(_that.fileTpl);
                data.files = _that.attachments;
                _that.container.find('.fileList').empty().append( render(data) ).find('.file span').click(
                    _that.removeFile
                );
            }



            this.uploaded = function(data){
                if( !data ){
                    _that.container.find('.note').text('上传文件类型超过支持范围，或超过最大允许文件大小');
                    return;
                }
                _that.attachments.push(data);
                var render = template.compile(_that.fileTpl);
                var data = {};
                data.files = _that.attachments;
                _that.container.find('.fileList').empty().append( render(data) ).find('.file span').click(
                    _that.removeFile
                );
                
            }

            this.exports = {
            	'message-file-uploaded' : this.uploaded
            }

        }
        return MessageBox;
    }
)