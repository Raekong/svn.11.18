/**
 * 
 */
define(
	["wangEditor.min", "xss.min", "i18next.min"],
	function( et, xss, it ){
		
		function Editor( domId, i18n, height, change ){
			this.domId = domId;
			this.editor = null;
			
			var menus = [
			    'head',  // 标题
			    'italic',  // 斜体
			    'list',  // 列表
			    'justify',  // 对齐方式
			    'undo',  // 撤销
			    'redo'  // 重复
			];

			
			this.init = function(){
				if( typeof this.domId == 'string' ){
					this.editor = new et('#' + this.domId );
				}else{
					this.editor = new et( this.domId );
				}
				this.editor.i18next = it
				this.editor.config.fontSizes = {
					'x-small': { name: '10px', value: '1' },
					'small': { name: '13px', value: '2' },
					'normal': { name: '13px', value: '3' },
					'large': { name: '18px', value: '4' },
					'x-large': { name: '24px', value: '5' },
					'xx-large': { name: '32px', value: '6' },
					'xxx-large': { name: '48px', value: '7' },
				}
				if(!!change) //编辑器输入触发事件
					this.editor.config.onchange = change;
				this.editor.config.height = height
				this.editor.config.lang = i18n ? 'zh-CN' : 'en';
				this.editor.config.fontSizes = {'normal': { name: '12px', value: '2' }};
				this.editor.config.menus = menus;
				this.editor.config.pasteFilterStyle = false;
				this.editor.config.pasteText= false;
				this.editor.config.pasteTextHandle =function (content){
				     if (content == '' && !content) return '';
				     var html = content;
				     html = html.replace(/<\/?SPANYES[^>]*>/gi, "");//  Remove  all  SPAN  tags
				     html = html.replace(/<(\w[^>]*)  lang=([^|>]*)([^>]*)/gi, "<$1$3");//  Remove  Lang  attributes
				     html = html.replace(/<\\?\?xml[^>]*>/gi, "");//  Remove  XML  elements  and  declarations
				     html = html.replace(/<\/?\w+:[^>]*>/gi, "");//  Remove  Tags  with  XML  namespace  declarations:  <o:p></o:p>
				     html = html.replace(/&nbsp;/, "");//  Replace  the  &nbsp;
				     html = html.replace(/\n(\n)*( )*(\n)*\n/gi, '\n');
				     return html;
				};
				this.editor.create();
				return this;
			}
			
			this.html = function(){
				return filterXSS(this.editor.txt.html());	
			}

			this.insert = function(tag, html){
				var content = $('#'+this.editor.txt.editor.textElemId);
				content.find(tag).append(html);
			}

			this.update = function(tag, html){
				var content = $('#'+this.editor.txt.editor.textElemId);
				content.find(tag).html(html);
			}
			
			this.txt = function(){
				return filterXSS(this.editor.txt.text());	
			}
			
			this.set = function( html ){
				this.editor.txt.html( html );
			}
			
			this.clear = function(){
				this.editor.clear();
			}

			this.scrollToHead = function(id){
				this.editor.scrollToHead(id);
			}

			
		}
		return Editor;
	}
)