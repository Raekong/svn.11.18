define(
    ["mailtpl","widget", "confirm", "uploader", "popup", "filedownloader"],
    function(mailtpl, wd, confirm, uploader, popup, downloader){
        function SimilarCheckTab(container, ec){
            this.name = 'SimilarCheckTab';
            this.container = container;
            this.ec = ec;
            this.ec.addHandler(this);
            this.wd = new wd();
            
            this.lang = $('#frame').attr('i18n');

            var _that = this;

            this.init = function(){
                this.wd.init( this.container.find('#similarcheck'), 'tab');
                this.container.find('#scfile .cell.file, #scfile .down , #previewfiles .cell.file, #previewfiles .down').click(
                    function(){
                        var dom = $(this).closest(".row");
                        var link = dom.attr('url');
                        var fileName = dom.attr('name');
                        new downloader().download(link, fileName);
                    }
                )

                this.container.find('.decisionbut').click(
                    _that.makeDecision
                );

                this.container.find('#upload').click(
                    _that.uploader
                )
            }

            
            _that.uploader = function(){
                var fileType = $('#checkfiletype').text();
                $('#frame').attr('rid', $(this).closest('[rid]').attr('rid'));
                $('#frame').attr('fileType', fileType);
                $.post(
                    '/submit/submit-upload-pop/' + (this.lang ? 'zh' : 'en'),
                    function(rst){
                        var dom = $(rst);
                        var title = dom.find('.title').text();
                        _that.pop = new popup(title, dom,  '788px', function(dom, index){
                            //去掉其它可选项
                            dom.find('.autolist li:not(:contains('+ fileType +'))').remove();
                            dom.find('#fileType').val(fileType).attr('readonly', 'readonly');
                            //去掉文件列表
                            dom.find('#uploadfiles').remove();
                            dom.find('#uploadDiv').attr('label', 'similarcheck-upload');
                            var uploader = _that.initUploader(dom.find('#uploadDiv'), _that.ec, null, (this.lang== 'true'));
                            uploader.init();

                            dom.find('#cancel').click(
                                function(){
                                    _that.pop.close();
                                }
                            )
                        });
                        _that.pop.pop();
                    }
                )
            }

            this.initUploader = function(dom, ec, uploadBut, lang){
                var up = new uploader(
                    dom, 
                    ec,
                    $('#uploadConfirm'),  //确定上传按键
                    lang
                );
                return up;
            }

            this.revision = function(data){
                //避开REVIEW TABLE上传文件的干扰
                if($('#similarcheck #upload').length == 0) return;
                if( data == null ){
                    $('.note').text('上传文件类型错误，或文件大小超过限定值');
                    return ;
                }
                data.rid = $('#frame').attr('rid');
                data.aid = $('#frame').attr('aid');
                data.jid = $('#frame').attr('jid');
                data.fileType = $('#frame').attr('fileType');
                
                $.post(
                    '/similarcheck/revision/upload',
                    data,
                    function(){
                        window.location.reload();
                    }
                )
            }

           
            _that.decisiondo = function(type, jid, aid, rid){
                var data = {};
                data.recvType = _that.mail.getRecvType();
                data.content = _that.mail.getContent();
                data.title = _that.mail.getTitle();
                data.type = type;
                data.rid = rid;
                data.jid = jid;
                data.aid = aid;

                $.post(
                    '/similarcheck/decision/done/',
                    data,
                    function(){
                        window.location.reload();
                    }
                )
            }
            
            _that.pass = function(jid, aid){
                _that.confirm = new confirm(
                    'Send to', 
                    'Are you sure sent the article to similarity check?', 
                    _that.lang=='true'? 'zh': 'en',  
                    function(dom){
                        dom.find('#cancel').click(function(){
                            _that.confirm.close();
                        })

                        dom.find('#save').click(function(){
                            $.post(
                                '/similarcheck/sendToSimilarCheck',
                                {jid:jid, aid:aid, rid:0},
                                function(){
                                    window.location.reload();
                                }
                            )
                        })
                    }, '488px');
                _that.confirm.confirm();
            }

            this.configPoint = {
                revision : 'Similar Checked Revision',
                decline : 'Similar Checked Decline',
                'pre-review decline': 'Decline Submission'
            }


            _that.makeDecision = function(){
                var rid = $(this).closest("[tag]").attr("rid");
                var jid = $('#frame').attr('jid');
                var aid = $('#frame').attr('aid');
                var type = $(this).attr('data');

                if(type=="pass"){
                    _that.pass(jid, aid, rid);
                    return;
                }
                console.log(type);
                $.post(
                    '/similarcheck/decision/'+type,
                    {"jid": jid },
                    function( rst ){
                        var dom = $(rst);
                        $('#tab').hide();
                        $('#emailcontainer').empty().append(dom).show();
                        $.post(
                            '/article/email/' ,
                            { 
                                aid:$('#frame').attr('aid'),  
                                jid: $('#frame').attr('jid'), 
                                configPoint: _that.configPoint[type], 
                                'i18n': ($('#frame').attr('lang')=='true' ? 'zh' : 'en') 
                            },
                            function( rst ){
                                var tplDom = $(rst);
                                _that.container.find('#mailtpl').append(tplDom);
                                _that.container.find('#mailtitle').append('<span style="font-weight:bold; color:red;"> [ '+ type +' ] </span>')
                                _that.mail = new mailtpl( _that.container.find('#mailtpl'), $('#frame').attr('lang')=='true').init();

                                var html = _that.mail.getContent().replace('#Similarity Revision Due#', $('.similarcheck-email').attr('revisiondue'));
                                _that.mail.setContent(html);
                            }
                        )

                        dom.find('#cancel').click(
                            function(){
                                $('#emailcontainer').empty().hide();
                                $('#tab').show();
                            }
                        );

                        dom.find('#save').click(
                            function(){
                                _that.decisiondo(type, jid, aid, rid);
                            }
                        );
                    }
                )
            }

            this.exports = {
                'similarcheck-upload' : this.revision
            }
        
        }

        return SimilarCheckTab;
    }
)