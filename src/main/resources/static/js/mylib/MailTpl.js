define(
    ["editor"],
    function(ed){
        function MailTpl(dom, isZh){
            this.dom = dom;
            
            var _that = this;

            this.editChange = function( html ){
                var tpl = _that.dom.find('.tplname span.checked').parent().attr('data');
                _that.dom.find('.tplbody[data="'+tpl+'"] .tplcontent').html(html);
                
            }
            // console.log( dom.html());
            this.ed = new ed('emailcontent', isZh, 388, this.editChange);
            this.ed.init();

            this.setRecvType = function( dom ){
                _that.dom.find('.recvs span.nocheck').removeClass('checked');
                dom.find('span.nocheck').addClass('checked');
            }

            this.setCurrentTpl = function( dom ){
                _that.dom.find('.tplname span.nocheck').removeClass('checked');
                dom.find('span.nocheck').addClass('checked');

                var body = dom.attr('data');
                var html = _that.dom.find('.tplbody[data="'+body+'"] .tplcontent').html();

                _that.dom.find('.tplbody').hide();
                _that.dom.find('.tplbody[data="'+body+'"]').show();
                _that.ed.set(html);
            }

            this.init = function(){
                var tpl = this.dom.find('.tplname[t=true]');
                this.setCurrentTpl(tpl);                

                this.dom.find('.tplname').click(
                    function(){
                        _that.setCurrentTpl( $(this));
                    }
                );

                this.dom.find('.recvs').click(
                    function(){
                        _that.setRecvType( $(this));
                    }
                );

                return this;
            }

            this.getTitle = function(){
                var tpl = _that.dom.find('.tplname span.checked').parent().attr('data');
                return _that.dom.find('.tplbody[data="'+tpl+'"] input').val();
            }

            this.getContent = function(){
                return this.ed.html();
            }

            this.getRecvType = function(){
                return _that.dom.find('.recvs span.checked').parent().attr('type');
            }

            this.setContent = function(content){
                return this.ed.set(content);
            }

            this.scrollTo = function(headId){
                this.ed.scrollToHead(headId)
            }


            this.insert = function(tag, html){
                this.ed.insert(tag, html);
            }

            this.update = function(tag, html){
                this.ed.update(tag, html);
            }
        }

        return MailTpl;
    }
)