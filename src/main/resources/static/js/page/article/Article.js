define(
    [ "homecommon" , "eventcenter", "widget", "submittab", "reviewtab", "history", "similarchecktab", "paymenttab", "copyedittab" ],
    function(hc, ec, wd, st, re, his, sck, pay, cpy){
        function Article( container ){
            this.name = "Article";
            this.container = container;
            this.hc = new hc(container);
            this.hc.init(
                function( menu ){
                    menu.find('li#3').addClass('current');
                }
            );

            this.lang = $('#frame').attr('i18n');
            this.wd = new wd();
            this.wd.init( this.container.find('#tabs'), 'tab');

            this.ec = new ec('debug');
            this.ec.addHandler( this ); 

            var _that = this;

            this.init = function(){
                _that.container.find('div.tabDiv').each(
                    function(){
                        var tag = $(this).attr('tag');
                        var dom = $(this);
                        _that.loadTab(tag, dom, _that.ec);
                    }
                )
                //自动下拉的直接关闭
                $('body').click(
                    function(){
                        $('.autolist').hide();
                    }
                )
            }

            this.loadTab = function(tag, dom, ec){
                $.post(
                    '/article/tab/'+$('#frame').attr('aid') + "/" +tag,
                    {jid: $('#frame').attr('jid') },
                    function(rst){
                        console.log(tag);
                        dom.empty().append(rst);
                        switch(tag){
                            case 'submit':
                                _that.submit = new st(dom, ec);
                                _that.submit.init();
                                break;
                            case 'review':
                                _that.review = new re(dom, ec);
                                _that.review.init();
                                break;
                            case 'history':
                                _that.history = new his(dom, ec);
                                _that.history.init();
                                break;
                            case 'similarcheck':
                                _that.sck = new sck(dom, ec);
                                _that.sck.init();
                                break;
                            case 'payment':
                                _that.pay = new pay(dom, ec);
                                _that.pay.init();
                                break;
                            case 'copyedit':
                                _that.cpy = new cpy(dom, ec);
                                _that.cpy.init();
                                break;
                        }
                    }
                )
            }

            this.reload = function(params){
                console.log(params);
                switch(params.tab){
                    case 'submit':
                        var dom = _that.container.find('div[tag='+params.tab+"]");
                        _that.loadTab(params.tag, dom, _that.ec);
                        break;
                    case 'review':
                        var dom = _that.container.find('div[tag='+params.tab+"]");
                        _that.loadTab(params.tag, dom, _that.ec);
                        break;
                }
            }

            this.reloadPage = function(){
                window.location.reload();
            }

            this.exports = {
                'reload' :  this.reload,
                'reloadPage' :  this.reloadPage,
            }
        }
        return Article;
    }
)