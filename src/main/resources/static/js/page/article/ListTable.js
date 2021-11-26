define(
    ["template"],
    function( template ){
        function Table(){
            this.title = {
                'id': { zh:'ID', en: 'ID'},
                'title': { zh:'标题', en: 'Title'},
                'js': { zh:'期刊与栏目', en: 'Journal&Section'},
                'timeStamp': { zh:'投稿时间', en: 'Submit Date'},
                'status': { zh:'状态', en: 'Status'}
            }

            var _that = this;

            this.render = function(i18n, tablehead, row){
                
                var tpl = '<div class="info">'
                tablehead.find('[data]').each(   
                    function(  ){
                        var key = $(this).attr('data');
                        var value = row.find('['+ key +']').attr(''+key);
                       
                        var item = "<div class='infoItem'><span class='key'>" + (_that.title[key][ i18n ? 'zh' : 'en'] + ":</span><span class='value'>"+ value + "</span></div>");
                        tpl += item;
                    }
                )

                tpl += "</div>";
                return tpl;
            }
        }

        return Table;
    }

)