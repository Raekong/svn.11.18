define(
    ["uploader", "popup"],
    function( uploader, popup ){
        function FileUploader(ec, upbutton, dom, delFileCB, uploadFileCB ){//面板
            this.name = "FILEUPLOADER";
            this.dom = dom;
            this.upbutton = upbutton;
            this.files = [];
            this.delFileCB = delFileCB;
            this.uploadFileCB = uploadFileCB;
            this.ec = ec;   
            this.ec.addHandler(this);
            var _that = this;

            this.init = function(){
                
                this.upbutton.click(
                    function(){
                        $.post(
                            '/submit/submit-upload-pop/' + (this.lang ? 'zh' : 'en'),
                            function(rst){
                                _that.uploaderPop(rst);
                            }
                        )
                    }
                );

                //=========文件的删除操作－－－－－－－－－－－－－－－－－－－－－－－
                this.dom.find('.row span.del').click(
                    function(){
                        var id = $(this).closest('.row').attr('id');
                        _that.delFileCB(id);
                    }
                )

            }

            this.initUploader = function(dom, ec, uploadBut, lang){
                var up = new uploader(
                    dom, 
                    this.ec,
                    null,  //确定上传按键
                    lang
                );
                return up;
            }

            this.delUploadFile = function( id ){
                $.post(
                    '/article/file/del/'+id,
                    function(rst){
                        _that.wd.msgbox(
                            _that.lang == 'true',
                            'info',
                            'Message',
                            '文件删除成功！',
                            function(){
                                _that.ec.fire(
                                    _that.name,
                                    'reload',
                                    {tag: 'submit', tab:'submit'}
                                )
                            }
                        )
                    }
                )
            }
            
    
            this.uploaderPop = function(rst){
                var dom = $(rst);
                dom.find('#uploadfiles').remove();
                dom.find('#footer #save').hide();
                        
                var title = dom.find('.title').text();
                _that.pop = new popup(title, dom,  '788px', function(dom, index){
                    var uploader = _that.initUploader(dom.find('#uploadDiv'), _that.ec, null, (this.lang== 'true'));
                    uploader.init();
                    
                    _that.ec.fire(
                        'uploadFile',
                        'newUpload',
                        null
                    )
    
                    dom.find('#uploadConfirm').click(
                        function(){
                            _that.getFileInfo(uploader, dom) ;     
                        }
                    )
    
                    dom.find('#fileType').click(
                        function(e){
                            dom.find('.autolist').show();
                            e.stopPropagation();    
                        }
                    )
    
                    dom.find('.autolist ul li').click(
                        function(e){
                            dom.find('#fileType').val($(this).text());
                            dom.find('.autolist').hide();
                        }
                    )
                    dom.find('#cancel').click(
                        function(){
                            _that.pop.close();
                        }
                    )
    
                });
                _that.pop.pop();
            };
    
            this.getFileInfo = function(up, dom){
                $('#fileType').removeClass('error');
    
                if( !$('#fileType').val()){
                    $('#fileType').addClass('error');
                    return;
                }
                
                up.upInterface.upload($('#fileType').val());
            }
    
            this.fileUploadedCB = function(){
                $.post(
                    '/article/file/upload',
                    file,
                    function(rst){
                        $('#cancel').click();
                        _that.ec.fire(
                            _that.name,
                            'reload',
                            {tag: 'submit', tab:'submit'}
                        )
                        
                    }
                )
            }

            this.close = function(){
                _that.pop.close();
            }
            
    
            this.uploaded = function(data){
                if( data == null ){
                    $('.note').text('上传文件类型错误，或文件大小超过限定值');
                    return ;
                }
                var file = data;
                file.filetype = $('#fileType').val();
                file.aid = $('#frame').attr('aid');
                file.rid = _that.dom.attr('rid');
                _that.currentFile = {};
                _that.uploadFileCB(file);
            }

            this.exports = {
                'uploaded': this.uploaded
            }
        }
        return FileUploader;
    }
)