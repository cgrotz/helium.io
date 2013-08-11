var rr = new Roadrunner('http://localhost:8080/roadrunner/repo');

var Tree = function(ref, element) {
	$(element).find('> li > span').on('click', function(e) {
		var children = $(element).find(' li > ul > li');
		if (children.is(":visible")) {
			children.hide('fast');
		} else {
			children.show('fast');
		}
		e.stopPropagation();
	});
	new TreeNode(ref, element, null);
};

var TreeNode = function(ref, parent, snapshot) {
	var self = this;
	this.parent = parent;
	this.ref = ref;
	this.name = ref.name();
	
	this.addNode = function(){
		$(self.parent).append(
				'<li id="' + self.name + '"><span>'
						+ '<i class="icon-folder-open"></i>' + self.name
						+ '</span>'
						+ '<ul></ul></li>');
		this.element = $('#' + self.name);
		this.element.find(' > ul').hide('fast');
		
		var loadSubTree = function(){
			if(!self.loaded)
			{
				ref.on('child_added', function(snapshot) {
					var name = snapshot.name();
					if (!$(self.element).hasClass('parent_li')) {
						$(self.element).addClass('parent_li');
					}
					new TreeNode(snapshot.ref(), self.element.find(' > ul'), snapshot);
				});
				ref.on('child_changed', function(snapshot) {
					$(self.childId + "> #" + snapshot.name());
				});
				ref.on('child_removed', function(snapshot) {
					$(self.childId + "> #" + snapshot.name()).remove();
				});
				self.loaded = true;
			}
		}
		
		this.element.find(' > span ').on('click', function(e) {
			var children = self.element.find(' > ul');
			if (children.is(":visible")) {
				children.hide('fast');
			} else {
				children.show('fast');
				loadSubTree();
			}
			e.stopPropagation();
		});
	};
	
	this.addValue = function(){
		$(self.parent).append(
				'<li id="' + self.name + '"><span>'
						+ self.name
						+ ': <span class="value">'+snapshot.val()+'</span></span>'
						+ '<ul></ul></li>');

		var span = $(self.parent).find('#' + self.name+' > .value');
		
		ref.on('value', function(snapshot) {
			span.html(snapshot.val());
		});
	};

	if(snapshot != null && !(snapshot.val() instanceof Object))
	{
		self.addValue();
	}
	else
	{
		self.addNode();
	}
};

$(function() {
	var tree = new Tree(rr, '#tree');
});