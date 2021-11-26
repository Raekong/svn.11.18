define(
    [ "mailtpl", "widget", "popup",  "msgbox", "template", "fileuploader", "filedownloader", "confirm" ],
    function( mailtpl, wd, popup, msgbox, tpl, fileuploader, downloader, confirm){
        function Copyedit( container , ec ){
            this.name = 'copyedit';
            this.container = container;
            this.ec = ec;
            this.ec.addHandler(this);
            this.wd = new wd();

            this.lang = $('#frame').attr('i18n');
            this.wd.init( this.container.find('#copyedittab'), 'tab');
            var _that = this;

            this.init = function(){
                new downloader(_that.container.find('#files')).init();

                _that.container.find('#sendAuthorMsg').click(
                    _that.msgAuthor
                )

                _that.container.find('.viewmsg').click(
                    _that.viewmsg
                )

                if( $("#copyedittab #uploadRevision").length > 0 ){
                    _that.fileuploader = new fileuploader(
                        _that.ec,
                        $("#uploadRevision") ,
                        _that.container.find('#revisionboard'), 
                        _that.delFileCB, _that.uploadFileCB
                    )
                    _that.fileuploader.init();
                
                    _that.container.find('#revisionboard #save').click(
                        _that.uploadRevision
                    )
                }

                _that.container.find('.decisionbut').click(
                    function(){
                        var type = $(this).attr('data');
                        var data = {   
                            "aid": $('#frame').attr('aid'), 
                            "rid": $(this).closest('[rid]').attr('rid'), 
                            "jid": $('#frame').attr('jid') 
                        };
                        switch(type){
                            case 'revision':
                                _that.revision(data, $(this).closest('[rid]').attr('rid'));
                                break;
                            case 'pass':
                                _that.pass(data);
                                break;
                        }
                    }
                )
            }
            //====----------------revision files------------------------
            this.delFileCB = function(){
                $(this).closest('.row').remove();
            }

            var fileTpl = '<div class="row"><div class="cell" style="flex: 1 1 25%; " type="{{filetype}}">{{filetype}}</div>'
                        + '<div class="cell" style="flex: 1 1 57%;" innerId="{{innerId}}", originName="{{originName}}">{{originName}}</div>'
                        + '<div class="cell" style="flex: 1 1 18%;;" ><span class="del"><i class="fa fa-close"></i></span></div>'
                        + '</div>';
                        
            this.uploadFileCB = function(  data ){
                var dom = tpl.compile(fileTpl)(data);
                _that.container.find('#revisionboard .body').append(dom);
                _that.container.find('#revisionboard .body .del').unbind().click(
                    _that.delFileCB
                );
                _that.fileuploader.close();
            }

            this.uploadRevision = function(){
                var rid = $(this).closest('[rid]').attr('rid');
                var aid = $('#frame').attr('aid');
                var rows = _that.container.find('#revisionboard .row');
                var files = [];

                rows.each(
                    function(){
                        var data = {};
                        data.fileType = $(this).find('[type]').attr("type");
                        data.innerId =  $(this).find('[innerId]').attr("innerId");
                        data.originName =  $(this).find('[originName]').attr("originName");
                        files.push( data );
                    }
                )
                
                _that.confirm = new confirm(
                    'Upload', 
                    'Are you sure to upload the files in the list?', 
                    _that.lang=='true'? 'zh': 'en',  
                    function(dom){
                        dom.find('#cancel').click(function(){
                            _that.confirm.close();
                        })
                       
                        var data = {aid:aid, rid:rid, files: JSON.stringify(files)};
                        console.log( data );

                        dom.find('#save').click(function(){
                            $.post(
                                '/copyedit/submitRevision',
                                data,
                                function(){
                                    window.location.reload();
                                }
                            )
                        })
                    }, '488px');
                _that.confirm.confirm();
            }
            //====----------------revision-------------------------------
            this.revision = function(data, rid){
                $.post(
                    '/copyedit/decision/revision',
                    data,
                    function( rst ){
                        var dom = $(rst);
                        $('#roundtab').hide();
                        $('#emailcontainer').empty().append(dom).show();
                        _that.decision(dom, 'revision', rid);
                    }
                )
            }
            this.pass = function(data){
                _that.confirm = new confirm(
                    'Copyedit End', 
                    'Is the article copyedit end?', 
                    _that.lang=='true'? 'zh': 'en',  
                    function(dom){
                        dom.find('#cancel').click(function(){
                            _that.confirm.close();
                        })
                        dom.find('#save').click(function(){
                            $.post(
                                '/copyedit/decision/pass',
                                data,
                                function( rst ){
                                    window.location.reload();
                                }
                            )
                        })
                    }, '488px');
                _that.confirm.confirm();
               
            }
            
            this.decision = function( dom, type, rid ){
                dom.find('#cancel').click(
                    function(){
                        $('#roundtab').show();
                        $('#emailcontainer').empty().hide();
                    }
                )

                dom.find('.row .del').click(
                    function(){
                        $(this).closest('.row').remove();
                    }
                );

                dom.find('#save').click(
                    function(){
                        _that.sendMail(dom, type, rid);
                    }
                )

                $.post(
                    '/article/email' ,
                    { 
                        aid:$('#frame').attr('aid'),  
                        jid: $('#frame').attr('jid'), 
                        configPoint: "Copyedit Requirement", 
                        'i18n': ($('#frame').attr('lang')=='true' ? 'zh' : 'en') 
                    },
                    function( rst ){
                        var tplDom = $(rst);
                        _that.container.find('#mailtpl').append(tplDom);
                        _that.container.find('#mailtitle').append('<span style="font-weight:bold; color:red;"> [ '+ type +' ] </span>')
                        _that.mail = new mailtpl( _that.container.find('#mailtpl'), $('#frame').attr('lang')=='true').init();
                    }
                )
            }

            this.sendMail = function( dom, type, rid ){
                var data = {};
                data.jid = $('#frame').attr('jid');
                data.recvType = _that.mail.getRecvType();
                data.content = _that.mail.getContent();
                data.title = _that.mail.getTitle();
                data.type = type;
                data.aid = $('#frame').attr('aid');
                data.rid = rid;
                
                var files = [];
                dom.find('#files .row').each(
                    function(){
                        var innerId = parent.attr('innerId');
                        var originName = parent.attr('originName');
                        files.push({innerId:innerId, originName:originName});
                    }
                )
                data.attachfiles = JSON.stringify(files);

                $.post(
                    '/copyedit/sendDecision',
                    data,
                    function(){
                        window.location.reload();
                    }
                )
            }

            //=====---------------Discuss---------------------------------
            this.viewmsg = function(){
                var mid = $(this).attr("data");
                $.post(
                    '/message/getMessage',
                    { id:mid },
                    function(res){
                        $.post(
                            '/message/message-pop',
                            function(rst){
                                var dom = $(rst);
                                var title = dom.find('.title').text();
                                _that.msgpop = new popup(title, dom,  '768px', function(dom, index){
                                    dom.find('#msg-title').val(res.title)
                                    dom.find('#msg-content').html(res.content.replace('\\n', "<br/>"))
                                    dom.find('#cancel').click(
                                        function(){
                                            _that.msgpop.close();
                                        }
                                    );
                                });
                                _that.msgpop.pop();
                                if(!!res.appendsJSONStr){
                                    _that.showFiles(res.appendsJSONStr);
                                }
                            }
                        )
                    }
                )
            }

            this.fileTpl = '{{each files as file }}<div class="row" ><div style="flex:1 1 100%; text-align:left;"><a href="{{file.path}}">{{file.fileName}}</a></div></div>{{/each}}';
            this.showFiles = function(appendsJSONStr){
                var file_list = $('#fileList')
                file_list.empty()
                var files = JSON.parse(appendsJSONStr);

                files.forEach(
                    function(d ){
                        d.innerName = d.path.split('/').reverse()[0];
                    }
                );

                var data = {}; data.files = files;
                var listdom = tpl.compile(_that.fileTpl)(data);
                file_list.append(listdom);

                file_list.find('span.nocheck').click(
                    function(){
                       if( $(this).hasClass('checked')) $(this).removeClass('checked');
                       else $(this).addClass('checked');
                    }
                )

                $('#downloadzip').click(
                    function(){
                        var selected = $('#fileList span.checked');
                        var tmp = [];
                        for(var i=0; i<selected.length; i++){
                            tmp.push(selected.eq(i).attr('data'));
                        }

                        var fileStr = tmp.join(";");
                        console.log(fileStr);
                        window.open("/message/download?appends="+fileStr+"&type=COS");
                    }
                )
            }
            this.msgAuthor = function(){
                var rid = $(this).closest('[rid]').attr('rid');
                var aid = $('#frame').attr('aid');
                var isAuthor = $(this).attr('author') == 'true';
                _that.recevier = {};
                _that.recevier.rid = rid;
                _that.recevier.aid = aid;
                _that.recevier.type = 1;
                _that.recevier.isAuthor = isAuthor;
                $.post(
                    '/user/user-message-pop',
                    function(rst){
                        var dom = $(rst);
                        var title = dom.find('.title').text();
                        
                        _that.msgpop = new popup(title, dom,  '768px', function(dom, index){  
                            dom.find('#withemail').parent().hide();
                            dom.find('#namelist').remove();
                            dom.attr('eventLabel', 'sendCopyeditMsg'); //设置消息发送的上传回调事件标签
                            var mb = new msgbox(_that.lang=='true'? 'zh':'en', dom, _that.ec);
                            mb.init();

                            dom.find('#cancel').click(
                                function(){
                                    _that.msgpop.close();
                                });
                            });
                            _that.msgpop.pop();

                        }
                )
            }

            this.sendmsg = function(data){
                data.recevier = JSON.stringify(_that.recevier);
                data = $.extend(data, _that.recevier);
                $.post(
                    '/article/copyedit/sendmessage',
                    data,
                    function(){
                        _that.wd.msgbox(_that.lang=='true', 'info', 'Discuss', 'Message has sent successful！');
                        _that.msgpop.close();
                        window.location.reload();                     
                    }
                )
                
            }

            this.exports = {
            	'sendCopyeditMsg' : this.sendmsg
            }
        }
        return Copyedit;
    }
)