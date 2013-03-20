package org.docear.plugin.core.workspace.controller;

import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOExceptionWithCause;
import org.docear.plugin.core.io.ReplacingInputStream;
import org.docear.plugin.core.workspace.actions.DocearProjectSettings;
import org.docear.plugin.core.workspace.creator.FolderTypeLibraryCreator;
import org.docear.plugin.core.workspace.creator.FolderTypeLiteratureRepositoryCreator;
import org.docear.plugin.core.workspace.creator.FolderTypeLiteratureRepositoryPathCreator;
import org.docear.plugin.core.workspace.creator.LinkTypeIncomingCreator;
import org.docear.plugin.core.workspace.creator.LinkTypeLiteratureAnnotationsCreator;
import org.docear.plugin.core.workspace.creator.LinkTypeMyPublicationsCreator;
import org.docear.plugin.core.workspace.creator.LinkTypeReferencesCreator;
import org.docear.plugin.core.workspace.model.DocearWorkspaceProject;
import org.docear.plugin.core.workspace.node.FolderTypeLibraryNode;
import org.docear.plugin.core.workspace.node.FolderTypeLiteratureRepositoryNode;
import org.docear.plugin.core.workspace.node.LinkTypeIncomingNode;
import org.docear.plugin.core.workspace.node.LinkTypeLiteratureAnnotationsNode;
import org.docear.plugin.core.workspace.node.LinkTypeMyPublicationsNode;
import org.docear.plugin.core.workspace.node.LinkTypeReferencesNode;
import org.docear.plugin.core.workspace.node.LiteratureRepositoryPathNode;
import org.freeplane.core.util.Compat;
import org.freeplane.core.util.LogUtils;
import org.freeplane.core.util.TextUtils;
import org.freeplane.features.link.LinkController;
import org.freeplane.plugin.workspace.URIUtils;
import org.freeplane.plugin.workspace.WorkspaceController;
import org.freeplane.plugin.workspace.components.menu.WorkspacePopupMenu;
import org.freeplane.plugin.workspace.model.AWorkspaceNodeCreator;
import org.freeplane.plugin.workspace.model.AWorkspaceTreeNode;
import org.freeplane.plugin.workspace.model.IResultProcessor;
import org.freeplane.plugin.workspace.model.project.AWorkspaceProject;
import org.freeplane.plugin.workspace.model.project.ProjectLoader;
import org.freeplane.plugin.workspace.nodes.FolderTypeMyFilesNode;
import org.freeplane.plugin.workspace.nodes.FolderVirtualNode;
import org.freeplane.plugin.workspace.nodes.LinkTypeFileNode;
import org.freeplane.plugin.workspace.nodes.ProjectRootNode;

public class DocearProjectLoader extends ProjectLoader {	
	private FolderTypeLibraryCreator folderTypeLibraryCreator;
	private FolderTypeLiteratureRepositoryCreator folderTypeLiteratureRepositoryCreator;
	private FolderTypeLiteratureRepositoryPathCreator folderTypeLiteratureRepositoryPathCreator;
	
	private LinkTypeIncomingCreator linkTypeIncomingCreator;
	private LinkTypeLiteratureAnnotationsCreator linkTypeLiteratureAnnotationsCreator;
	private LinkTypeMyPublicationsCreator linkTypeMyPublicationsCreator;
	private LinkTypeReferencesCreator linkTypeReferencesCreator;
	private IResultProcessor resultProcessor;
	
	
	//DOCEAR - required for backwards compatibility   
//	private final static String CONFIG_FILE_NAME = "workspace.xml";
	

	/***********************************************************************************
	 * CONSTRUCTORS
	 **********************************************************************************/
	public DocearProjectLoader() {
		super();	
		initDocearReadManager();	
	}

	/***********************************************************************************
	 * METHODS
	 **********************************************************************************/
	private void initDocearReadManager() {		
		registerTypeCreator(ProjectLoader.WSNODE_FOLDER, FolderTypeLibraryCreator.FOLDER_TYPE_LIBRARY, getFolderTypeLibraryCreator());
		registerTypeCreator(ProjectLoader.WSNODE_FOLDER, FolderTypeLiteratureRepositoryCreator.FOLDER_TYPE_LITERATUREREPOSITORY, getFolderTypeLiteratureRepositoryCreator());
		registerTypeCreator(ProjectLoader.WSNODE_FOLDER, FolderTypeLiteratureRepositoryPathCreator.REPOSITORY_PATH_TYPE, getFolderTypeLiteratureRepositoryPathCreator());
		
		registerTypeCreator(ProjectLoader.WSNODE_LINK, LinkTypeIncomingCreator.LINK_TYPE_INCOMING, getLinkTypeIncomingCreator());
		registerTypeCreator(ProjectLoader.WSNODE_LINK, LinkTypeLiteratureAnnotationsCreator.LINK_TYPE_LITERATUREANNOTATIONS, getLinkTypeLiteratureAnnotationsCreator());
		registerTypeCreator(ProjectLoader.WSNODE_LINK, LinkTypeMyPublicationsCreator.LINK_TYPE_MYPUBLICATIONS, getLinkTypeMyPublicationsCreator());
		registerTypeCreator(ProjectLoader.WSNODE_LINK, LinkTypeReferencesCreator.LINK_TYPE_REFERENCES, getLinkTypeReferencesCreator());
	}
	
	private AWorkspaceNodeCreator getFolderTypeLiteratureRepositoryPathCreator() {
		if (folderTypeLiteratureRepositoryPathCreator == null) {
			folderTypeLiteratureRepositoryPathCreator = new FolderTypeLiteratureRepositoryPathCreator();
		}
		return folderTypeLiteratureRepositoryPathCreator;
	}
	
	private FolderTypeLibraryCreator getFolderTypeLibraryCreator() {
		if (folderTypeLibraryCreator == null) {
			folderTypeLibraryCreator = new FolderTypeLibraryCreator();
		}
		return folderTypeLibraryCreator;
	}

	private FolderTypeLiteratureRepositoryCreator getFolderTypeLiteratureRepositoryCreator() {
		if (folderTypeLiteratureRepositoryCreator == null) {
			folderTypeLiteratureRepositoryCreator = new FolderTypeLiteratureRepositoryCreator();
		}
		return folderTypeLiteratureRepositoryCreator;
	}
	
	

	private LinkTypeIncomingCreator getLinkTypeIncomingCreator() {
		if (linkTypeIncomingCreator == null) {
			linkTypeIncomingCreator = new LinkTypeIncomingCreator();
		}
		return linkTypeIncomingCreator;
	}

	private LinkTypeLiteratureAnnotationsCreator getLinkTypeLiteratureAnnotationsCreator() {
		if (linkTypeLiteratureAnnotationsCreator == null) {
			linkTypeLiteratureAnnotationsCreator = new LinkTypeLiteratureAnnotationsCreator();
		}
		return linkTypeLiteratureAnnotationsCreator;
	}

	private LinkTypeMyPublicationsCreator getLinkTypeMyPublicationsCreator() {
		if (linkTypeMyPublicationsCreator == null) {
			linkTypeMyPublicationsCreator = new LinkTypeMyPublicationsCreator();
		}
		return linkTypeMyPublicationsCreator;
	}

	private LinkTypeReferencesCreator getLinkTypeReferencesCreator() {
		if (linkTypeReferencesCreator == null) {
			linkTypeReferencesCreator = new LinkTypeReferencesCreator();
		}
		return linkTypeReferencesCreator;
	}
	
	public synchronized LOAD_RETURN_TYPE loadProject(AWorkspaceProject project) throws IOException {
		try {
			File projectSettings = new File(URIUtils.getAbsoluteFile(project.getProjectDataPath()),"settings.xml");
			if(projectSettings.exists()) {
				getDefaultResultProcessor().setProject(project);
				load(projectSettings.toURI());
				return LOAD_RETURN_TYPE.EXISTING_PROJECT;
			}
			else {
				createDefaultProject((DocearWorkspaceProject)project);
				return LOAD_RETURN_TYPE.NEW_PROJECT;
			}
		}
		catch (Exception e) {
			throw new IOExceptionWithCause(e);
		}
	}
	
	public IResultProcessor getDefaultResultProcessor() {
		if(this.resultProcessor == null) {
			this.resultProcessor = new DocearResultProcessor();
		}
		return this.resultProcessor;
	}
	
	private void createDefaultProject(DocearWorkspaceProject project) {
		File home = URIUtils.getFile(project.getProjectHome());
		if(!home.exists()) {
			home.mkdirs();
		}
		DocearProjectSettings settings = (DocearProjectSettings) project.getExtensions(DocearProjectSettings.class);
		ProjectRootNode root = new ProjectRootNode();
		root.setProjectID(project.getProjectID());				
		root.setModel(project.getModel());
		root.setName(home.getName());
		
		project.getModel().setRoot(root);
		
		
		File _dataInfoFile = new File(URIUtils.getFile(project.getProjectDataPath()).getParentFile(), "!!!info.txt");
		if(!_dataInfoFile.exists()) {
			createAndCopy(_dataInfoFile, "/conf/!!!info.txt");
		}
		
		// create and load all default nodes
		FolderTypeLibraryNode libNode = new FolderTypeLibraryNode();
		libNode.setName(TextUtils.getText(libNode.getClass().getName().toLowerCase(Locale.ENGLISH)+".label" ));
		project.getModel().addNodeTo(libNode, root);
		
		URI libPath = project.getRelativeURI(project.getProjectLibraryPath());
		
		LinkTypeIncomingNode incomNode = new LinkTypeIncomingNode();
		incomNode.setLinkPath(URIUtils.createURI(libPath.toString()+"/incoming.mm"));
		incomNode.setName(TextUtils.getText(incomNode.getClass().getName().toLowerCase(Locale.ENGLISH)+".label" ));
		project.getModel().addNodeTo(incomNode, libNode);
		
		LinkTypeLiteratureAnnotationsNode litNode = new LinkTypeLiteratureAnnotationsNode();
		litNode.setLinkPath(URIUtils.createURI(libPath.toString()+"/literature_and_annotations.mm"));
		litNode.setName(TextUtils.getText(litNode.getClass().getName().toLowerCase(Locale.ENGLISH)+".label" ));
		project.getModel().addNodeTo(litNode, libNode);
		
		LinkTypeMyPublicationsNode pubNode = new LinkTypeMyPublicationsNode();
		pubNode.setLinkPath(URIUtils.createURI(libPath.toString()+"/my_publications.mm"));
		pubNode.setName(TextUtils.getText(pubNode.getClass().getName().toLowerCase(Locale.ENGLISH)+".label" ));
		project.getModel().addNodeTo(pubNode, libNode);
		
		LinkTypeFileNode tempNode = new LinkTypeFileNode();
		tempNode.setLinkURI(URIUtils.createURI(libPath.toString()+"/temp.mm"));
		tempNode.setName(TextUtils.getText(tempNode.getClass().getName().toLowerCase(Locale.ENGLISH)+".temp.label" ));
		tempNode.setSystem(true);
		project.getModel().addNodeTo(tempNode, libNode);
		
		LinkTypeFileNode trashNode = new LinkTypeFileNode();
		trashNode.setLinkURI(URIUtils.createURI(libPath.toString()+"/trash.mm"));
		trashNode.setName(TextUtils.getText(trashNode.getClass().getName().toLowerCase(Locale.ENGLISH)+".trash.label" ));
		trashNode.setSystem(true);
		project.getModel().addNodeTo(trashNode, libNode);
				
		FolderVirtualNode refs = new FolderVirtualNode() {
			private static final long serialVersionUID = 1L;

			public WorkspacePopupMenu getContextMenu() {
				return null;
			}
			
			public boolean acceptDrop(DataFlavor[] flavors) {
				return false;
			}
		};
		refs.setName(TextUtils.getText(FolderTypeMyFilesNode.class.getPackage().getName().toLowerCase(Locale.ENGLISH)+".refnode.name"));
		refs.setSystem(true);
		project.getModel().addNodeTo(refs, root);
		
		LinkTypeReferencesNode defaultRef = new LinkTypeReferencesNode();
		//use default bib file
		if(settings != null && settings.getBibTeXLibraryPath() != null) {
			File file = URIUtils.getFile(settings.getBibTeXLibraryPath());
			defaultRef.setName(file.getName());
			defaultRef.setLinkURI(project.getRelativeURI(settings.getBibTeXLibraryPath()));
		}
		else {
			defaultRef.setName(TextUtils.getText(FolderTypeMyFilesNode.class.getPackage().getName().toLowerCase(Locale.ENGLISH)+".refnode.name"));
			defaultRef.setLinkURI(URIUtils.createURI(libPath.toString()+"/default.bib"));
		}
		project.getModel().addNodeTo(defaultRef, refs);
		
				
		FolderTypeLiteratureRepositoryNode litRepoNode = new FolderTypeLiteratureRepositoryNode();
		litRepoNode.setSystem(true);		
		project.getModel().addNodeTo(litRepoNode, root);
		if(settings != null) { 
			if(settings.useDefaultRepositoryPath()) {
				LiteratureRepositoryPathNode pathNode = new LiteratureRepositoryPathNode();
				pathNode.setPath(URIUtils.createURI(libPath.toString()+"/literature_repository"));
				pathNode.setName(TextUtils.getText(pathNode.getClass().getName().toLowerCase(Locale.ENGLISH)+".default.label" ));
				pathNode.setSystem(true);
				project.getModel().addNodeTo(pathNode, litRepoNode);
			}
			for (URI uri : settings.getRepositoryPathURIs()) {
				LiteratureRepositoryPathNode pathNode = new LiteratureRepositoryPathNode();
				File file = URIUtils.getFile(uri);
				pathNode.setPath(project.getRelativeURI(uri));
				pathNode.setName(file.getName());
				pathNode.setSystem(true);
				project.getModel().addNodeTo(pathNode, litRepoNode);
			}
		}		
		
		root.initiateMyFile(project);
		
		FolderVirtualNode misc = new FolderVirtualNode();
		misc.setName(TextUtils.getText(FolderTypeMyFilesNode.class.getPackage().getName().toLowerCase(Locale.ENGLISH)+".miscnode.name"));
		project.getModel().addNodeTo(misc, root);
		
		File _welcomeFile = new File(URIUtils.getFile(WorkspaceController.getApplicationHome()), "docear-welcome.mm");
		URI welcomeURI = project.getRelativeURI(_welcomeFile.toURI());
		LinkTypeFileNode welcomeNode = new LinkTypeFileNode();
		welcomeNode.setLinkURI(welcomeURI);
		welcomeNode.setName(_welcomeFile.getName());
		project.getModel().addNodeTo(welcomeNode, misc);
		
		if(settings != null && settings.includeDemoFiles()) {
			LiteratureRepositoryPathNode pathNode = new LiteratureRepositoryPathNode();
			URI uri = URIUtils.createURI(libPath.toString()+"/Example%20PDFs");
			File file = URIUtils.getAbsoluteFile(uri);
			pathNode.setPath(uri);
			pathNode.setName(file.getName());
			pathNode.setSystem(true);
			project.getModel().addNodeTo(pathNode, litRepoNode);
			LogUtils.info("copy docear tutorial files");
			copyDemoFiles(project, URIUtils.getAbsoluteFile(defaultRef.getLinkURI()), (settings.getBibTeXLibraryPath() == null));
		}
		
		//misc -> help.mm
		root.refresh();
		
		
	}
	
	private void copyDemoFiles(DocearWorkspaceProject project, File bibPath, boolean setBib) {
		//prepare paths
		File defaultFilesPath = URIUtils.getFile(project.getProjectLibraryPath());
		defaultFilesPath.mkdirs();
		File repoPath = new File(defaultFilesPath, "Example PDFs");
		repoPath.mkdirs();
		URI relativeRepoPath = project.getRelativeURI(repoPath.toURI());
		
		//prepare replace map
		Map<String, String> replaceMapping = new HashMap<String, String>();
		replaceMapping.put("@LITERATURE_REPO_DEMO@", relativeRepoPath.toString());
		
		//DOCEAR - todo: test
		URI relativeBibURI = LinkController.toLinkTypeDependantURI(bibPath, repoPath, LinkController.LINK_RELATIVE_TO_MINDMAP);
		if(Compat.isWindowsOS() && relativeBibURI.getPath().startsWith("//")) {
			replaceMapping.put("@LITERATURE_BIB_DEMO@", (new File(relativeBibURI).getPath().replace(File.separator, File.separator+File.separator)/*+File.separator+File.separator+"Example PDFs"*/));
		}
		else {
			replaceMapping.put("@LITERATURE_BIB_DEMO@", relativeBibURI.getPath().replace(":", "\\:")/*+"/Example PDFs"*/);
		}
		
		boolean created = createAndCopy(new File(defaultFilesPath,"incoming.mm"), "/demo/template_incoming.mm", replaceMapping);
		createAndCopy(new File(defaultFilesPath,"literature_and_annotations.mm"), "/demo/template_litandan.mm", replaceMapping);
		createAndCopy(new File(defaultFilesPath,"my_publications.mm"), "/demo/template_mypubs.mm", replaceMapping);
		createAndCopy(new File(defaultFilesPath,"temp.mm"), "/demo/template_temp.mm", created, replaceMapping);
		createAndCopy(new File(defaultFilesPath,"trash.mm"), "/demo/template_trash.mm", created, replaceMapping);
		if(setBib) {
			createAndCopy(bibPath, "/demo/docear_example.bib", true, replaceMapping);
		}
		
		createAndCopy(new File(URIUtils.getFile(project.getProjectHome()), "My New Paper.mm"), "/demo/docear_example_project/My New Paper.mm", replaceMapping);
		
		createAndCopy(new File(repoPath, "Academic Search Engine Optimization (ASEO) -- Optimizing Scholarly Literature for Google Scholar and Co.pdf"), "/demo/docear_example_pdfs/Academic Search Engine Optimization (ASEO) -- Optimizing Scholarly Literature for Google Scholar and Co.pdf");
		createAndCopy(new File(repoPath, "Academic search engine spam and Google Scholars resilience against it.pdf"), "/demo/docear_example_pdfs/Academic search engine spam and Google Scholars resilience against it.pdf");
		createAndCopy(new File(repoPath, "An Exploratory Analysis of Mind Maps.pdf"), "/demo/docear_example_pdfs/An Exploratory Analysis of Mind Maps.pdf");
		createAndCopy(new File(repoPath, "Docear -- An Academic Literature Suite.pdf"), "/demo/docear_example_pdfs/Docear -- An Academic Literature Suite.pdf");
		createAndCopy(new File(repoPath, "Google Scholar's Ranking Algorithm -- An Introductory Overview.pdf"), "/demo/docear_example_pdfs/Google Scholar's Ranking Algorithm -- An Introductory Overview.pdf");
		createAndCopy(new File(repoPath, "Google Scholar's Ranking Algorithm -- The Impact of Citation Counts.pdf"), "/demo/docear_example_pdfs/Google Scholar's Ranking Algorithm -- The Impact of Citation Counts.pdf");
		createAndCopy(new File(repoPath, "Information Retrieval on Mind Maps -- What could it be good for.pdf"), "/demo/docear_example_pdfs/Information Retrieval on Mind Maps -- What could it be good for.pdf");
		createAndCopy(new File(repoPath, "Mr. DLib -- A Machine Readable Digital Library.pdf"), "/demo/docear_example_pdfs/Mr. DLib -- A Machine Readable Digital Library.pdf");
	}
	
	private boolean createAndCopy(File file, String resourcePath) {
		return createAndCopy(file, resourcePath, false, null);
	}
	
	private boolean createAndCopy(File file, String resourcePath,final Map<String, String> replaceMap) {
		return createAndCopy(file, resourcePath, false, replaceMap);
	}
	
	private boolean createAndCopy(File file, String resourcePath, boolean force,final Map<String, String> replaceMap) {
		try {
			if(!file.exists() || force) {
				createFile(file);
				InputStream is = this.getClass().getResourceAsStream(resourcePath);
				if(replaceMap == null) {
					FileUtils.copyInputStreamToFile(is, file);
				}
				else {
					FileUtils.copyInputStreamToFile(new ReplacingInputStream(replaceMap, is), file);
				}
				return true;
			}			
		}
		catch (Exception e) {
			LogUtils.warn(e);
		}	
		return false;
	}
	
	/**
	 * @param file
	 * @throws IOException
	 */
	private void createFile(File file) throws IOException {
		if(!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
			return;
		}
		file.createNewFile();
	}
	
	/***********************************************************************************
	 * NESTED CLASSES
	 **********************************************************************************/
	
	private class DocearResultProcessor implements IResultProcessor {

		private AWorkspaceProject project;

		public AWorkspaceProject getProject() {
			return project;
		}

		public void setProject(AWorkspaceProject project) {
			this.project = project;
		}

		public void process(AWorkspaceTreeNode parent, AWorkspaceTreeNode node) {
			if(getProject() == null) {
				LogUtils.warn("Missing project container! cannot add node to a model.");
				return;
			}
			if(node instanceof ProjectRootNode) {
				getProject().getModel().setRoot(node);
				if(((ProjectRootNode) node).getProjectID() == null) {
					((ProjectRootNode) node).setProjectID(getProject().getProjectID());
				}
				
			}
			else {
				if(parent == null) {
					if (!getProject().getModel().containsNode(node.getKey())) {
						getProject().getModel().addNodeTo(node, parent);			
					}
				}
				else {
					if (!parent.getModel().containsNode(node.getKey())) {
						parent.getModel().addNodeTo(node, parent);			
					}
				}
				//add myFiles after a certain node type
//				if(node instanceof FolderTypeLibraryNode)
				if(node instanceof FolderTypeLiteratureRepositoryNode)
				{
					((ProjectRootNode) parent.getModel().getRoot()).initiateMyFile(getProject());
				}
			}
		}

	}
}