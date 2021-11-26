define(
    ["widget", "popup", "confirm", "template", 
     "msgbox", "reviewer", "decision", "fileuploader", "filedownloader"],
    function(wd,  popup, confirm, tpl, msgbox, re, ds, uploader, downloader){
        function ReviewTab( container, ec ){
            this.name = 'reviewtab';
            this.container = container;
            this.ec = ec;
            this.ec.addHandler(this);
            this.wd = new wd();
            this.lang = $('#frame').attr('i18n');

            this.lang = $('#frame').attr('i18n');
            this.wd.init( this.container.find('#reviewtab'), 'tab');

            var _that = this;
            this.init = function(){
                _that.container.find('.decisionbut').click(
                    _that.makeDecision
                )

                _that.container.find('#files span.del').click(
                    _that.delReviewFiles
                )

                //作者查看到转刊建议时的，处理按键 
                _that.container.find('span[change]').click(
                    _that.changeDecision
                )

                _that.container.find('#assignEditor').click(
                    _that.assignEditor
                )

                _that.container.find('.delEditor').click(
                    _that.delEditor
                )

                _that.container.find('.msgEditor').click(
                    _that.msgEditor
                )

                _that.container.find('#sendAuthorMsg').click(
                    _that.msgAuthor
                )

                _that.container.find('.viewmsg').click(
                    _that.viewmsg
                )

                _that.container.find('#reviewerassign').click(
                    _that.reviewerInvite
                )

                _that.container.find('.reviewaction span.close').click(
                    _that.closeReview
                )

                _that.container.find('.reviewaction span.withdraw').click(
                    _that.withdrawReview
                )

                _that.container.find('.reviewaction span.view').click(
                    _that.viewReview
                )

                _that.container.find('.reviewaction span.remind').click(
                    _that.remindReview
                )

                _that.container.find('#revisionboard #save').click(
                    _that.submitRevision
                )

                //初始化下载文件
                new downloader(_that.container.find('#files')).init();
                //用户自己修改上传文件
               
                if( $("#reviewtab #upload").length > 0 ){
                    _that.reviewFileUploader = new uploader(
                        _that.ec,
                        $("#reviewtab #upload") ,
                        _that.container.find('#files'), 
                        _that.delFileCB, _that.uploadReviewFile
                    )
                    _that.reviewFileUploader.init();
                }
                


                //上传REVISION或者用户修改文件-that
                if( $("#reviewtab #uploadRevision").length > 0 ){
                    _that.uploader = new uploader(
                        _that.ec,
                        $("#uploadRevision") ,
                        _that.container.find('#revisionboard'), 
                        _that.delFileCB, _that.uploadFileCB
                    )
                    _that.uploader.init();
                }
            }
            //==============Review Files Modify==================================
            _that.delReviewFiles = function(){
                var fileId = $(this).closest('.row').attr('id');
                _that.confirm = new confirm(
                    '删除文件', 
                    '确定删除选中文件?', 
                    _that.lang=='true'? 'zh': 'en',  
                    function(dom){
                        dom.find('#cancel').click(function(){
                            _that.confirm.close();
                        })
                        dom.find('#save').click(function(){
                            $.post(
                                '/article/file/del/'+fileId,
                                function(){
                                    window.location.reload();
                                }
                            )
                        })
                    }, '488px');
                _that.confirm.confirm();
            }

            _that.uploadReviewFile = function(data){
                console.log(data);
                $.post(
                    '/article/file/upload',
                    data,
                    function(rst){
                        if( rst == 'true' ){
                            window.location.reload();
                        }else{
                            _that.wd.msgbox(
                                _that.lang == 'true',
                                'alert',
                                '消息',
                                'A file of the same type has been uploaded. Please confirm the type of uploaded file.'
                            )
                        }
                    }
                )
            }
           
            

            //==============SUBMIT Revision======================================
            _that.submitRevision = function(){
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
                    '提交修改稿', 
                    '确定提交修改稿及相关附件?', 
                    _that.lang=='true'? 'zh': 'en',  
                    function(dom){
                        dom.find('#cancel').click(function(){
                            _that.confirm.close();
                        })
                       
                        var data = {aid:aid, rid:rid, files: JSON.stringify(files)};
                        console.log( data );

                        dom.find('#save').click(function(){
                            $.post(
                                '/review/submitRevision/',
                                data,
                                function(){
                                    window.location.reload();
                                }
                            )
                        })
                    }, '488px');
                _that.confirm.confirm();
            }

            //==============revision file uploader================================
            _that.delFileCB = function(){
                $(this).closest('.row').remove();
            }

            var fileTpl = '<div class="row"><div class="cell" style="flex: 1 1 25%; " type="{{filetype}}">{{filetype}}</div>'
                        + '<div class="cell" style="flex: 1 1 57%;" innerId="{{innerId}}", originName="{{originName}}">{{originName}}</div>'
                        + '<div class="cell" style="flex: 1 1 18%;;" ><span class="del"><i class="fa fa-close"></i></span></div>'
                        + '</div>';
                        
            _that.uploadFileCB = function(  data ){
                var dom = tpl.compile(fileTpl)(data);
                _that.container.find('#revisionboard .body').append(dom);
                _that.container.find('#revisionboard .body .del').unbind().click(
                    _that.delFileCB
                );
                _that.uploader.close();
            }

            //-=============Review Action==========================================
            _that.remindReview = function(){
                var raid = $(this).closest('.row').attr('raid');
                _that.confirm = new confirm(
                    '审稿提醒', 
                    '确定提醒审稿人审稿?', 
                    _that.lang=='true'? 'zh': 'en',  
                    function(dom){
                        dom.find('#cancel').click(function(){
                            _that.confirm.close();
                        })

                        dom.find('#save').click(function(){
                            $.post(
                                '/review/sendRemind/'+raid,
                                function(){
                                    window.location.reload();
                                }
                            )
                        })
                    }, '488px');
                _that.confirm.confirm();
            }
            
            _that.viewReview = function(){
                var raid = $(this).closest('.row').attr('raid');
                $.post(
                    '/review/getLinkMd5/'+raid,                    
                    function( rst ){
                        window.open( "/link/" + rst + "?other=true" );
                    }
                )
            }

            _that.withdrawReview = function(){
                var raid = $(this).closest('.row').attr('raid');
                _that.confirm = new confirm(
                    '撤消', 
                    '撤消该审稿操作并返回到上次状态?', 
                    _that.lang=='true'? 'zh': 'en',  
                    function(dom){
                        dom.find('#cancel').click(function(){
                            _that.confirm.close();
                        })

                        dom.find('#save').click(function(){
                            $.post(
                                '/review/withdraw/'+raid,
                                function(){
                                    window.location.reload();
                                }
                            )
                        })
                    }, '488px');
                _that.confirm.confirm();
            }

            _that.closeReview = function(){
                var raid = $(this).closest('.row').attr('raid');
                var aid = $('#frame').attr('aid');
                _that.confirm = new confirm(
                    '关闭审稿', 
                    '确定终止该审稿活动并通知审稿人?', 
                    _that.lang=='true'? 'zh': 'en',  
                    function(dom){
                        dom.find('#cancel').click(function(){
                            _that.confirm.close();
                        })

                        dom.find('#save').click(function(){
                            $.post(
                                '/review/close/'+raid,
                                {aid: aid},
                                function(){
                                    window.location.reload();
                                }
                            )
                        })
                    }, '488px');
                _that.confirm.confirm();
            }


            //==============REVIEW INVIE============================================
            _that.reviewerInvite = function(){
                var rid = $(this).closest('[rid]').attr('rid');
                
                $.post(
                    '/article/review/review-invite-pop',
                    {aid: $('#frame').attr('aid'), rid: rid, jid: $('#frame').attr('jid')},
                    function( rst ){
                        var dom = $(rst);
                        dom.find('#save').attr('rid', rid);
                        var title = dom.find('.title').text();
                        new re( _that.ec, dom).init();
                        _that.container.find('#roundtab').hide();
                        _that.container.find('#reviewertab').append(dom).show();
                    }
                )
            }

            this.returnreviewer = function(){
                _that.container.find('#reviewertab').empty().hide();
                _that.container.find('#roundtab').show();
                document.body.scrollTop = document.documentElement.scrollTop = 0;
            }

            this.nullReviewer = function(){
                _that.wd.msgbox(
                    _that.lang == 'true',
                    'alert',
                    '添加审稿人',
                    '请添加审稿人再发送审稿邀请'
                )
            }

            //==============EDITOR MSG AND DISCUSSION===============================
            this.sendmsg = function(data){
                if( _that.recevier.email){//是作者之间的讨论
                    data.recevier = JSON.stringify(_that.recevier);
                    data = $.extend(data, _that.recevier);
                    console.log(data);  
                    $.post(
                        '/article/review/sendmessage',
                        data,
                        function(){
                            _that.wd.msgbox(_that.lang=='true', 'info', '消息', '消息已经发送成功！');
                            _that.msgpop.close();
                            _that.ec.fire(
                                'review',
                                'reloadPage',
                                null
                            )
                        }
                    )
                }else{
                    data = $.extend(data, _that.recevier);
                    $.post(
                        '/article/review/sendDisscuss',
                        data,
                        function(){
                            _that.wd.msgbox(_that.lang=='true', 'info', '消息', '消息已经发送成功！');
                            _that.msgpop.close();
                            _that.ec.fire(
                                'review',
                                'reloadPage',
                                null
                            )
                        }
                    )
                }
            }

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
                var isAuthor = $(this).attr('author') == 'true';
                var aid = $('#frame').attr('aid');
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

            this.msgEditor = function(){
                var email =  $(this).attr('email');
                var name =  $(this).attr('name');
                var id = $(this).attr('uid');
                var rid = $(this).closest('[rid]').attr('rid');
                var aid = $('#frame').attr('aid');
                _that.recevier = {};
                _that.recevier.email = email; 
                _that.recevier.name = name;
                _that.recevier.rid = rid;
                _that.recevier.recvid = id;
                _that.recevier.aid = aid;
                _that.recevier.type = 2;
                
                $.post(
                    '/user/user-message-pop',
                    function(rst){
                        var dom = $(rst);
                        var title = dom.find('.title').text();
                        _that.msgpop = new popup(title, dom,  '768px', function(dom, index){    

                            dom.find('#recevier').text( name +' [ '+ email +' ]')
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
            //==============EDITOR BOARD MANGER===============================
            this.delEditor = function(){
                var aid = $('#frame').attr('aid');
                var email = $(this).attr('data');
                var name = $(this).closest('.editor').find('.editorname').text();
                var cont = '确定从审稿编辑团队中删除' + name + '['+email+'] ?';
                _that.confirm = new confirm(
                    '删除编辑', 
                    cont, 
                    _that.lang=='true'? 'zh': 'en',  
                    function(dom){
                        dom.find('#cancel').click(function(){
                            _that.confirm.close();
                        })

                        dom.find('#save').click(function(){
                            _that.delEditorDo(aid, email);
                        })
                    }, '488px');
                _that.confirm.confirm();
            }

            this.delEditorDo = function(aid, email){
                $.post(
                    '/article/review/delEditor',
                    {aid:aid, email: email},
                    function(){
                        _that.ec.fire(
                            'review',
                            'reloadPage',
                            null
                        )
                    }
                )
            }

            this.assignEditor = function(){
                var rid = $(this).closest('[rid]').attr('rid');
                $.post(
                    '/article/review/assign-editor-pop',
                    {aid: $('#frame').attr('aid')},
                    function( rst ){
                        var dom = $(rst);
                        dom.find('#save').attr('rid', rid);
                        var title = dom.find('.title').text();
                        _that.pop = new popup(title, dom,  '548px', function(dom){
                            dom.find('.nocheck').click(
                                function(){
                                    if($(this).hasClass('checked')){
                                        $(this).removeClass('checked');
                                    }else{
                                        $(this).addClass('checked');
                                    }
                                }
                            )

                            dom.find('#save').click(
                                function(){
                                    var uid = dom.find('.autoinput').attr('data');
                                    var authority = dom.find('.nocheck').hasClass('checked');
                                    
                                    var data = {aid:$('#frame').attr('aid') , uid:uid, authority:authority};
                                    //console.log(data);
                                    if(!!uid){
                                        $.post(
                                            '/article/review/addEditor',
                                            {rid: rid, aid:$('#frame').attr('aid') , uid:uid, authority:authority},
                                            function(){
                                                _that.ec.fire(
                                                    'review',
                                                    'reloadPage',
                                                    null
                                                )
                                            }
                                        )
                                    }else{
                                        _that.wd.msgbox(
                                            _that.lang == 'true',
                                            'alert',
                                            '添加编辑',
                                            '请输入待添加的编辑邮箱'
                                        )
                                    }
                                }
                            )

                            _that.wd.autoComplete(
                                dom.find('#user'), 
                                '/user/getSectionEditorByEmailAndJid/' + $('#frame').attr('jid'), 
                                "<ul>{{each list as data}}<li class='autoitem' data={{data.userId}}>{{data.email}}</li>{{/each}}</ul>", 
                                function(data){  
                                    var rst = [];
                                    data.forEach(
                                        function(d){
                                            if(!d.disabled){
                                                rst.push(d);
                                            }
                                        }
                                    )
                                    return rst;
                                },
                                function(){},
                                function(){}             
                            );


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
            //==============DICSION==============================
            this.makeDecision = function(){
                var aid = $("#frame").attr("aid");
                var jid = $("#frame").attr("jid");
                var rid = $(this).closest("[tag]").attr("rid");
                var type = $(this).attr('data');
                switch(type){
                    case 'preview':
                        _that.previewPasswd();
                        break; 
                    case 'decline':
                    case 'accept':
                    case 'revision':
                        $.post(
                            '/review/decision/'+type,
                            {"aid":aid, "rid":rid, "jid": jid},
                            function( rst ){
                                var dom = $(rst);
                                $('#roundtab').hide();
                                $('#reviewertab').empty().append(dom).show();
                                new ds(dom, _that.ec, type, rid).init();
                            }
                        )
                        break; 
                    case 'sugaccept':
                    case 'sugdecline':
                        _that.suggest( rid, type );
                        break;
                    case 'change':
                        _that.change( rid );
                        break;
                    case 'withdraw':
                        _that.withdraw( aid, rid );
                        break;
                }
            }
            _that.withdraw = function( aid, rid ){
                $.post(
                    "/review/withdrawdecision/"
                    ,{aid: aid, rid: rid},
                    function(rst){
                        window.location.reload();
                    }
                )
            }

            this.previewPasswd = function(){
                _that.confirm = new confirm(
                    '论文预审', 
                    '确定论文通过预审?', 
                    _that.lang=='true'? 'zh': 'en',  
                    function(dom){
                        dom.find('#cancel').click(function(){
                            _that.confirm.close();
                        })

                        dom.find('#save').click(function(){
                            _that.sendPreviewPass();
                        })
                    });
                _that.confirm.confirm();
            }

            this.sendPreviewPass=function(){
                var aid = $('#frame').attr('aid');
                $.post(
                    "/article/review/passReview",
                    { aid : aid },
                    function(){
                        $('#cancel').click();
                        _that.ec.fire(
                            'review',
                            'reloadPage',
                            null
                        )
                    }
                )
            }

            this.suggest = function( rid, type ){
                var cont = (type == 'sugaccept' ? '建议接收该论文' : '建议拒收该论文' );
                _that.confirm = new confirm(
                    '处理建议', 
                    cont, 
                    _that.lang=='true'? 'zh': 'en',  
                    function(dom){
                        dom.find('#cancel').click(function(){
                            _that.confirm.close();
                        })

                        dom.find('#save').click(function(){
                            _that.sendSuggest(rid, cont);
                            _that.ec.fire(
                                'review',
                                'reloadPage',
                                null
                            )
                        })
                    });
                _that.confirm.confirm();
            }

            this.sendSuggest = function(rid, cont){
                var aid = $('#frame').attr('aid');
                $.post(
                    "/article/review/suggest",
                    { aid : aid, rid:rid, suggest: cont },
                    function(){
                        $('#cancel').click();
                        _that.ec.fire(
                            'review',
                            'reloadPage',
                            null
                        )
                    }
                )
            }

            this.changeDecision = function(){
                var type = $(this).attr('class');
                var jid = $(this).attr('change').split(',')[0];
                var sid = $(this).attr('change').split(',')[1];
                var rid = $(this).closest('[rid]').attr('rid');
                var aid = $('#frame').attr('aid');
                if( type == 'disagree'){    
                    _that.sendDecline(rid);
                    return;
                }

                if( type == 'agree'){
                    var data = {};
                    data.jid = jid; data.sid=sid; data.rid=rid; data.aid=aid;
                    _that.resubmit(data);
                }
            }

            this.resubmit = function(data){
                $.post(
                    '/article/review/changedo',
                    data,
                    function(){
                        _that.ec.fire(
                            'review',
                            'reloadPage',
                            null
                        )
                    }
                )
            }

            this.change = function( rid ){
                $.post(
                    '/article/review/review-change-pop',
                    function( rst ){
                        var dom = $(rst);
                        //在DOM中隐藏RID
                        dom.find('#save').attr('rid', rid);
                        var title = dom.find('.title').text();
                        _that.pop = new popup(title, dom,  '548px', _that.changeSend);
                        _that.pop.pop();
                    }
                )
            }

            this.initSections = function(dom, jid ){
                var tplstr = "<ul>{{each list as data}}<li class='autoitem' data={{data.id}}>{{data.title}}</li>{{/each}}</ul>";
                dom.find('#section .autoinput').attr('sid', '').val('');
                $.post(
                    '/journal/getAllSectionsByJid',
                    {jid: jid},
                    function(rst){  
                        var data = {};
                        data.list = JSON.parse(rst);
                        dom.find('#section .autolist').empty()
                            .append( tpl.compile(tplstr)(data))
                            .find('li').click(
                                function(e){
                                    dom.find('#section .autoinput').val($(this).text()).attr('sid', $(this).attr('data'));
                                    dom.find('#section .autolist').hide();
                                    e.stopPropagation();    
                                }
                            );
                        dom.find('#section .autoinput').click(
                            function(e){
                                dom.find('#section .autolist').show();
                                e.stopPropagation();  
                            }
                        )

                    }
                )
            }
            this.changeJournalDo = function(jid, sid, rid){
                var data = {jid:jid, sid:sid, aid:$('#frame').attr('aid'), rid:rid };
                console.log(data);
                $.post(
                    '/article/review/change',
                    {jid:jid, sid:sid, aid:$('#frame').attr('aid'), rid:rid },
                    function(rst){
                        _that.pop.close();
                        _that.ec.fire(
                            'review',
                            'reloadPage',
                            null
                        )
                    }
                )
            }
            
            this.changeSend = function(dom){
                var rid = dom.attr('rid');
                _that.wd.autoComplete(
                    dom.find('#journal'), 
                    '/journal/querybyAbbrLike', 
                    "<ul>{{each list as data}}<li class='autoitem' data={{data.journalId}}>{{data.title}}</li>{{/each}}</ul>", 
                    function(data){ 
                        return data;
                    },
                    function(){},
                    function(item){
                        _that.initSections(dom, item.attr('data'));
                    }             
                );
                
                dom.find('#save').click(
                    function(){
                        var jid = dom.find('#journal .autoinput').attr('data');
                        var sid = dom.find('#section .autoinput').attr('sid');
                        var rid = $(this).attr('rid');
                        if( !!jid && !!sid){
                            _that.changeJournalDo(jid, sid, rid);
                        }else{
                            _that.wd.msgbox(
                                _that.lang == 'true',
                                'alert',
                                '建议转刊',
                                '请设定论文转刊的期刊与栏目'
                            )
                        }
                       
                    }
                )

                dom.find('#cancel').click(
                    function(){
                        _that.pop.close();
                    }
                )
            }


            this.exports = {
            	'sendmsg' : this.sendmsg,
                'returnreviewer' : this.returnreviewer,
                'selectReviewer': this.reviewerSelect,
                'nullreviewer' : this.nullReviewer
            }
        }

        return ReviewTab;
    }
)