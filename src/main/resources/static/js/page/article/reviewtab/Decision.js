define(
    [ "mailtpl", "widget", "template"],
    function(mailtpl, wd, tpl){
        function Decision( container, ec, type, rid){
            this.name = "Decision";
            this.container = container;
            this.type = type;
            this.rid = rid;
            this.ec = ec;
            this.ec.addHandler(this);
            this.wd = new wd();
            
            var _that = this;
            this.lang = $('#frame').attr('i18n');

            this.init = function(){
                
                this.container.find('#cancel').click(
                    function(){
                        _that.ec.fire(
                            'decision',
                            'returnreviewer'
                            ,null
                        )
                    }
                )
                //------INIT EMILTPL==========================----------------------
                var configPoint = "";
                switch(type){
                    case 'decline': configPoint = "Decline Submission";
                        break;
                    case 'accept': configPoint = "Article Accept";
                        break;
                    case 'revision': configPoint = "Article Revision";
                        break;
                }
                console.log(type);
                $.post(
                    '/article/email' ,
                    { 
                        aid:$('#frame').attr('aid'),  
                        jid: $('#frame').attr('jid'), 
                        configPoint: configPoint, 
                        'i18n': ($('#frame').attr('lang')=='true' ? 'zh' : 'en') 
                    },
                    function( rst ){
                        var tplDom = $(rst);
                        _that.container.find('#mailtpl').append(tplDom);
                        _that.container.find('#mailtitle').append('<span style="font-weight:bold; color:red;"> [ '+ type +' ] </span>')
                        _that.mail = new mailtpl( _that.container.find('#mailtpl'), $('#frame').attr('lang')=='true').init();
                        
                    }
                )
                //------INIT FileDown ==========================----------------------
                this.container.find('.cell.file').click(
                    function(){
                        var url = $(this).attr('url');
                        var fileName = $(this).text(); 
                        _that.download(url, fileName);
                    }
                )

                //------INIT File Copy ==========================----------------------
                this.container.find('.file span.copy').click(
                    function(){
                        var parent = $(this).closest('.row');
                        var innerId = parent.attr('innerId');
                        var originName = parent.attr('originName');
                        _that.addAttache({innerId:innerId, originName:originName});
                    }
                )
                
                //------REVIEW RECOMMED ==========================----------
                var recommendTpl = '<div id="review{{index}}"><span style="font-weight: bold"> [ Reviewer {{index}} ]</span><div data="{{type}}" >{{content}}</div></div>';
                this.container.find('.action .copy').click(
                    function(){
                        var index = $(this).closest('.action').attr('index');
                        var content = $(this).closest('.resulttxt').find('.content').text();
                        var type = $(this).attr('data');
                        
                        var rendhtml = tpl.compile(recommendTpl)({type:type, index:index, content:content});
                        _that.mail.insert('#reviewboard', rendhtml);
                        _that.mail.scrollTo('review'+index);
                        var element =document.querySelector("#attachs")
                        let top = element.offsetTop;   //相对于页面顶部的距离
                        window.scrollTo(0, top -80);
                    }
                )

                this.container.find('#save').click(
                    _that.sendMail
                )

                //======---------INIT PAYMENT-------------------------------
                if( this.container.find('#payment').length>0 ){
                    this.container.find('#payment div[cell]').each(
                        function(){
                            _that.wd.init($(this), 'input');
                        }
                    )

                    this.container.find('#payment input:not([data-totalpage])')
                        .css('background-color', '#f2f2f2')
                        .attr('readonly', 'readonly');

                    this.container.find('#payment input[data-totalpage]').keyup(
                        function(){
                            var totalPage = $(this).val();
                            if(! /^(\-)?[0-9]+$/.test(totalPage) ){
                                $(this).addClass('error');
                                return;
                            }else{
                                $(this).removeClass('error');
                                $.post(
                                    '/payment/countAndApc',
                                    {aid: $('#frame').attr('aid'), jid: $('#frame').attr('jid'), 'pnum': totalPage},
                                    function(rst){
                                        _that.container.find('#payment input[data-totalapc]').val(rst.totalAPC);
                                        _that.container.find('#payment input[data-wiretrnasfer]').val(rst.totalWire);
                                        _that.container.find('#payment input[data-online]').val(rst.totalOnline);
                                        _that.container.find('#payment').attr('linkmde5',rst.linkmd5);
                                        _that.mail.update('#apc', rst.apcinfo);
                                    }
                                )
                            }

                        }
                    )
                }

                //--------========-INIT ACCEPT SEND FILE NEXT----------------------------
                this.container.find('#nextStage .row .del').click(
                    function(){
                        $(this).closest('.row').remove();
                    }
                );

            }

            this.sendMail = function(){
                var data = {};
                data.jid = $('#frame').attr('jid');
                data.recvType = _that.mail.getRecvType();
                data.content = _that.mail.getContent();
                data.title = _that.mail.getTitle();
                data.attachs = JSON.stringify(_that.attachs);
                data.type = _that.type;
                data.aid = $('#frame').attr('aid');
                data.rid = _that.rid;
                if( _that.container.find('#nextStage').length > 0){
                    var nextStageFiles = [];
                    _that.container.find('#nextStage .row').each(
                        function(){
                            nextStageFiles.push( $(this).attr('id'));
                        }
                    )
                    data.nextStageFiles = JSON.stringify(nextStageFiles);
                }

                if( _that.container.find('input[data-totalpage]').length > 0){
                    data.pagenum = _that.container.find('input[data-totalpage]').val();
                    if(!data.pagenum){
                        _that.wd.msgbox(
                            _that.lang == 'true',
                            'alert',
                            'APC Setting',
                            'Please input total page number of Arcticle'
                        )

                        return;
                    }
                    //支付外链
                    data.linkmd5 = _that.container.find('#payment').attr('linkmde5');
                }

                //console.log(data);
                $.post(
                    '/review/sendDecision',
                    data,
                    function(){
                        window.location.reload();
                    }
                )

            }

            this.attachs = [];

            this.addAttache = function(data){
                var flag = false;
                for(var i=0; i<_that.attachs.length; i++){
                    if( data.innerId == _that.attachs[i].innerId){
                        flag = true;
                        break;
                    }
                }

                if(flag){
                    _that.wd.msgbox($('#frame').attr('lang') == 'true', 'alert', '添加附件', '文件已经添加到附件列表中')
                    return;
                }

                _that.attachs.push( data );
                _that.renderAttachs(_that.attachs);
                var element =document.querySelector("#attachs")
                let top = element.offsetTop;   //相对于页面顶部的距离
                window.scrollTo(0, top -80);
            }


            this.attachsTpl = '{{each list as file }}<div class="row" originName={{file.originName}}, innerId={{file.innerId}}>'
                             + '<div  class="cell file" style="flex: 1 1 90%" cell="90%" "> {{file.originName}}</div>'
                             + '<div  class="cell" style="flex: 1 1 10%; text-align:left" cell="10%" ><span class="del"><i class="fa fa-close"></i></span></div>'
                             + '</div>{{/each}}';

            this.renderAttachs = function(files){
                var data = {}; data.list = files;
                var dom = tpl.compile(_that.attachsTpl)(data);
                _that.container.find('#attachs .body').empty().append(dom);
                _that.container.find('#attachs .body span.del').click(
                    _that.removeAttachs
                )
            }
            _that.removeAttachs = function(){
                var innerId = $(this).closest('.row').attr('innerId');
                var i=0;
                for(; i<_that.attachs.length; i++){
                    if( innerId == _that.attachs[i].innerId){
                        flag = true;
                        break;
                    }
                }

                _that.attachs.splice( i, 1 );
                _that.renderAttachs(_that.attachs);
            }

            _that.download = function(url, filename){
                var xhr = new XMLHttpRequest();
                xhr.open('GET', url, true);
                xhr.responseType = 'blob';
                xhr.onload = function () {
                    var blob = xhr.response;
                    if (xhr.status === 200) {
                        if (window.navigator.msSaveOrOpenBlob) {
                            navigator.msSaveBlob(blob, filename);
                        }else {
                            var link = document.createElement('a');
                            var body = document.querySelector('body');
                            link.href = window.URL.createObjectURL(blob);
                            link.download = filename;
                            // fix Firefox
                            link.style.display = 'none';
                            body.appendChild(link);
                            link.click();
                            body.removeChild(link);
                            window.URL.revokeObjectURL(link.href);
                        };
                    }
                };
                xhr.send();
            }
        }
        return Decision;
    }
)