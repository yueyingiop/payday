const state = {
  files: [], fileByPath: new Map(), folderMeanings: {}, systems: [],
  folderTree: null, selectedFolder: "", selectedFile: null, mapScale: 1
};

document.addEventListener("DOMContentLoaded", () => {
  loadData().then(initialize).catch(showLoadError);
});

async function loadData() {
  const embedded = window.PAYDAY_MINDMAP_DATA;
  let result;
  if (window.location.protocol === "file:" && embedded) {
    result = [embedded.files, embedded.folderMeanings, embedded.systems];
  } else {
    try {
      result = await Promise.all([
        fetchJson("payday_java_files.json"),
        fetchJson("payday_java_folder_meanings.json"),
        fetchJson("payday_java_systems.json")
      ]);
    } catch (error) {
      if (!embedded) throw error;
      result = [embedded.files, embedded.folderMeanings, embedded.systems];
    }
  }
  state.files = result[0];
  state.fileByPath = new Map(state.files.map(file => [file.path, file]));
  state.folderMeanings = result[1];
  state.systems = result[2];
  state.folderTree = buildFolderTree(state.files);
}

async function fetchJson(path) {
  const response = await fetch(path, { cache: "no-store" });
  if (!response.ok) throw new Error(path + " 加载失败：" + response.status);
  return response.json();
}

function initialize() {
  renderSystemList();
  renderFolderList();
  window.addEventListener("hashchange", renderRoute);
  renderRoute();
  document.getElementById("zoomIn")?.addEventListener("click", () => changeZoom(1.15));
  document.getElementById("zoomOut")?.addEventListener("click", () => changeZoom(.87));
  document.getElementById("resetView")?.addEventListener("click", () => { state.mapScale = 1; renderCurrentMap(); });
}

function getRoute() {
  const params = new URLSearchParams(window.location.hash.slice(1));
  return { system: params.get("system") || "", folder: params.get("folder") || "" };
}

function renderRoute() {
  const route = getRoute();
  renderSystemList();
  if (route.system) renderSystemPage(route.system);
  else renderOverviewPage(route.folder);
}

function buildFolderTree(files) {
  const root = { path: "", name: "PayDay", files: [], folders: new Map() };
  for (const source of files) {
    const parts = source.path.split("/");
    const fileName = parts.pop();
    let current = root;
    let path = "";
    for (const part of parts) {
      path = path ? path + "/" + part : part;
      if (!current.folders.has(part)) current.folders.set(part, { path, name: part, files: [], folders: new Map() });
      current = current.folders.get(part);
    }
    current.files.push({ ...source, name: fileName, folder: current.path });
  }
  return root;
}

function folderCount(folder) {
  return folder.files.length + [...folder.folders.values()].reduce((sum, child) => sum + folderCount(child), 0);
}

function getFolder(path) {
  if (!path) return state.folderTree;
  let current = state.folderTree;
  for (const part of path.split("/")) current = current?.folders.get(part);
  return current;
}

function renderFolderList() {
  const container = document.getElementById("folderList");
  if (!container) return;
  container.replaceChildren();
  container.append(createFolderRow(state.folderTree, 0, true));
  const children = document.createElement("div");
  children.className = "tree-children";
  for (const folder of [...state.folderTree.folders.values()].sort(sortByName)) children.append(createFolderBranch(folder, 0));
  container.append(children);
}

function createFolderBranch(folder, level) {
  const branch = document.createElement("div");
  branch.className = "tree-branch";
  const row = createFolderRow(folder, level, false);
  branch.append(row);
  const children = document.createElement("div");
  children.className = "tree-children";
  children.hidden = true;
  for (const file of [...folder.files].sort(sortByName)) children.append(createFileRow(file, level + 1));
  for (const child of [...folder.folders.values()].sort(sortByName)) children.append(createFolderBranch(child, level + 1));
  const toggle = row.querySelector(".tree-toggle");
  if (children.childElementCount) {
    toggle.addEventListener("click", event => {
      event.stopPropagation();
      children.hidden = !children.hidden;
      event.currentTarget.textContent = children.hidden ? "▸" : "▾";
    });
  }
  branch.append(children);
  return branch;
}

function createFolderRow(folder, level, isRoot) {
  const row = document.createElement("div");
  row.className = "tree-row";
  row.dataset.folder = folder.path;
  row.style.paddingLeft = (6 + level * 8) + "px";
  const toggle = document.createElement("button");
  toggle.className = "tree-toggle"; toggle.type = "button";
  toggle.textContent = isRoot ? "▾" : (folder.folders.size || folder.files.length ? "▸" : "·");
  toggle.disabled = !isRoot && !folder.folders.size && !folder.files.length;
  toggle.setAttribute("aria-label", "展开或收起 " + folder.name);
  const label = document.createElement("span"); label.className = "tree-label";
  label.textContent = isRoot ? "全部文件" : folder.name;
  const count = document.createElement("span"); count.className = "tree-count";
  count.textContent = folderCount(folder);
  row.append(toggle, label, count);
  row.addEventListener("click", () => selectFolder(folder.path));
  return row;
}

function createFileRow(file, level) {
  const row = document.createElement("div");
  row.className = "tree-row file-row";
  row.style.paddingLeft = (6 + level * 8) + "px";
  const toggle = document.createElement("button");
  toggle.className = "tree-toggle"; toggle.disabled = true; toggle.textContent = "·";
  const label = document.createElement("span"); label.className = "tree-label"; label.textContent = file.name;
  row.append(toggle, label);
  row.addEventListener("click", event => { event.stopPropagation(); selectFile(file.path); });
  return row;
}

function renderSystemList() {
  const container = document.getElementById("systemList");
  if (!container) return;
  container.replaceChildren();
  const activeId = getRoute().system;
  for (const system of state.systems) {
    const link = document.createElement("a");
    link.className = "system-link" + (system.id === activeId ? " active" : "");
    link.href = "#system=" + encodeURIComponent(system.id);
    link.innerHTML = "<strong>" + escapeHtml(system.title) + " (" + system.count + ")</strong><span>" + escapeHtml(system.description) + "</span>";
    container.append(link);
  }
}

function renderOverviewPage(folderPath) {
  state.activeSystemId = "";
  document.getElementById("pageTitle").textContent = "PayDay Java 文件思维导图";
  document.getElementById("pageSubtitle").textContent = "按目录查看文件、子目录和内部引用关系";
  document.title = "PayDay Java 文件思维导图";
  selectFolder(folderPath || "", false);
}

function renderSystemPage(systemId) {
  const system = state.systems.find(item => item.id === systemId);
  if (!system) return showLoadError(new Error("未找到系统：" + systemId));
  state.activeSystemId = systemId;
  document.getElementById("pageTitle").textContent = system.title;
  document.getElementById("pageSubtitle").textContent = system.description;
  document.title = "PayDay Java 思维导图 - " + system.title;
  document.getElementById("breadcrumb").textContent = "系统 / " + system.title;
  renderSystemMap(system);
  showSystemDetails(system);
}

function selectFolder(path, updateRoute = true) {
  state.selectedFolder = path; state.selectedFile = null;
  if (updateRoute) {
    const nextHash = path ? "#folder=" + encodeURIComponent(path) : "";
    if (window.location.hash !== nextHash) window.location.hash = nextHash;
    else renderRoute();
    return;
  }
  document.querySelectorAll(".tree-row[data-folder]").forEach(row => row.classList.toggle("active", row.dataset.folder === path));
  const folder = getFolder(path);
  document.getElementById("breadcrumb").textContent = path ? "目录 / " + path : "目录 / PayDay";
  renderOverviewMap(folder); showFolderDetails(folder);
}

function selectFile(path) {
  const file = state.fileByPath.get(path);
  if (!file) return;
  state.selectedFile = path;
  document.querySelectorAll(".tree-row[data-folder]").forEach(row => row.classList.remove("active"));
  document.getElementById("breadcrumb").textContent = "文件 / " + file.path;
  if (state.activeSystemId) {
    const system = state.systems.find(item => item.id === state.activeSystemId);
    if (system) renderSystemMap(system);
  } else {
    renderOverviewMap(getFolder(file.folder));
  }
  showFileDetails(file);
}

function renderCurrentMap() {
  if (state.activeSystemId) {
    const system = state.systems.find(item => item.id === state.activeSystemId);
    if (system) renderSystemMap(system);
  } else renderOverviewMap(getFolder(state.selectedFolder));
}

function changeZoom(factor) {
  state.mapScale = Math.max(.55, Math.min(1.7, state.mapScale * factor));
  renderCurrentMap();
}

function renderOverviewMap(folder) {
  const svg = document.getElementById("map");
  if (!svg || !folder) return;
  const items = [
    ...[...folder.folders.values()].sort(sortByName).map(data => ({ type: "folder", data })),
    ...[...folder.files].sort(sortByName).map(data => ({ type: "file", data }))
  ];
  const title = folder.path || "PayDay";
  const subtitle = folder.path ? (state.folderMeanings[folder.path] || "当前目录的直接文件和子目录") : "根目录的直接文件和一级目录";
  const positions = layoutOverview(items);
  const height = Math.max(560, positions.height + 170);
  setupSvg(svg, 1120, height);
  const content = [svgText(560, 34, title, "map-title", "middle"), svgText(560, 56, subtitle, "map-subtitle", "middle")];
  content.push(svgNode(rootNode(460, 78, 200, 54, { name: title, path: folder.path }), false));
  for (const item of items) {
    const position = positions.nodes.get(item.data.path || item.data.folder);
    content.push(svgLine(560, 132, position.x + position.w / 2, position.y, "contains"));
    content.push(svgNode(item.type === "folder" ? folderNode(position, item.data) : fileNode(position, item.data), true));
  }
  for (const item of items.filter(item => item.type === "file")) {
    const from = positions.nodes.get(item.data.path);
    for (const link of item.data.links || []) {
      const to = positions.nodes.get(link);
      if (to) content.push(svgLine(from.x + from.w, from.y + from.h / 2, to.x, to.y + to.h / 2, "relation", true));
    }
  }
  svg.innerHTML += content.join("");
  svg.querySelectorAll("[data-folder]").forEach(node => node.addEventListener("click", () => selectFolder(node.dataset.folder)));
  svg.querySelectorAll("[data-file]").forEach(node => node.addEventListener("click", () => selectFile(node.dataset.file)));
}

function layoutOverview(items) {
  const nodes = new Map();
  const columns = Math.max(1, Math.min(4, items.length));
  const width = 220, gap = 28, rowHeight = 102;
  const totalWidth = columns * width + (columns - 1) * gap;
  const startX = (1120 - totalWidth) / 2;
  items.forEach((item, index) => {
    const row = Math.floor(index / columns), column = index % columns;
    const w = item.type === "folder" ? 190 : 210;
    nodes.set(item.data.path || item.data.folder, { x: startX + column * (width + gap) + (width - w) / 2, y: 165 + row * rowHeight, w, h: 64 });
  });
  return { nodes, height: 165 + Math.ceil(items.length / columns) * rowHeight };
}

function renderSystemMap(system) {
  const svg = document.getElementById("map");
  if (!svg) return;
  const grouped = new Map();
  for (const path of system.files) {
    const file = state.fileByPath.get(path);
    if (!file) continue;
    const folder = file.folder || "根目录";
    if (!grouped.has(folder)) grouped.set(folder, []);
    grouped.get(folder).push(file);
  }
  const positions = new Map(), groups = [...grouped.entries()].sort((a, b) => a[0].localeCompare(b[0]));
  const layouts = [];
  let cursorY = 150;
  for (const [folder, groupFiles] of groups) {
    const columns = Math.min(4, Math.max(1, groupFiles.length)), nodeW = 215, gap = 22;
    const groupX = 45, groupW = 1030, rows = Math.ceil(groupFiles.length / columns), groupH = 62 + rows * 78;
    const startX = groupX + (groupW - (columns * nodeW + (columns - 1) * gap)) / 2;
    const nodes = [];
    groupFiles.sort(sortByName).forEach((file, index) => {
      const row = Math.floor(index / columns), column = index % columns;
      const position = { x: startX + column * (nodeW + gap), y: cursorY + 43 + row * 78, w: nodeW, h: 58 };
      positions.set(file.path, position); nodes.push({ file, position });
    });
    layouts.push({ folder, x: groupX, y: cursorY, w: groupW, h: groupH, nodes });
    cursorY += groupH + 22;
  }
  setupSvg(svg, 1120, Math.max(560, cursorY + 30));
  const content = [svgText(560, 34, system.title, "map-title", "middle"), svgText(560, 56, system.description, "map-subtitle", "middle")];
  content.push(svgNode(rootNode(460, 78, 200, 54, { name: system.title, path: "" }), false));
  for (const group of layouts) {
    content.push('<rect class="group-box" x="' + group.x + '" y="' + group.y + '" width="' + group.w + '" height="' + group.h + '"></rect>');
    content.push(svgText(group.x + 16, group.y + 24, group.folder, "group-label", "start"));
    content.push(svgLine(560, 132, group.x + group.w / 2, group.y, "contains"));
  }
  for (const group of layouts) for (const item of group.nodes) content.push(svgNode(fileNode(item.position, item.file), true));
  const selected = new Set(system.files);
  for (const path of system.files) {
    const from = positions.get(path), file = state.fileByPath.get(path);
    if (!from || !file) continue;
    for (const link of file.links || []) {
      const to = positions.get(link);
      if (selected.has(link) && to) content.push(svgLine(from.x + from.w / 2, from.y + from.h, to.x + to.w / 2, to.y, "relation", true));
    }
  }
  svg.innerHTML += content.join("");
  svg.querySelectorAll("[data-file]").forEach(node => node.addEventListener("click", () => showFileDetails(state.fileByPath.get(node.dataset.file))));
}

function setupSvg(svg, width, height) {
  svg.setAttribute("viewBox", "0 0 " + width + " " + height);
  svg.setAttribute("preserveAspectRatio", "xMidYMin meet");
  svg.style.transform = "scale(" + state.mapScale + ")";
  svg.innerHTML = '<defs><marker id="arrow" markerWidth="8" markerHeight="8" refX="7" refY="3" orient="auto"><path d="M0,0 L0,6 L7,3 z" fill="#e7b86a"></path></marker></defs><rect class="map-background" x="0" y="0" width="' + width + '" height="' + height + '"></rect>';
}

function svgNode(node, clickable) {
  let attrs = "";
  if (clickable) attrs = node.type === "folder" ? ' data-folder="' + escapeAttr(node.data.path) + '"' : ' data-file="' + escapeAttr(node.data.path) + '"';
  return '<g class="' + (clickable ? "node-clickable" : "") + '"' + attrs + ">" + node.svg + "</g>";
}

function rootNode(x, y, w, h, data) {
  return { type: "root", data, svg: '<rect class="node-box" x="' + x + '" y="' + y + '" width="' + w + '" height="' + h + '"></rect>' + svgText(x + w / 2, y + 24, data.name, "node-name", "middle") + svgText(x + w / 2, y + 42, data.path || "根目录", "node-path", "middle") };
}

function folderNode(position, folder) {
  return { type: "folder", data: folder, svg: '<rect class="node-box folder" x="' + position.x + '" y="' + position.y + '" width="' + position.w + '" height="' + position.h + '"></rect>' + svgText(position.x + position.w / 2, position.y + 26, folder.name, "node-name", "middle") + svgText(position.x + position.w / 2, position.y + 45, folderCount(folder) + " 个文件", "node-path", "middle") };
}

function fileNode(position, file) {
  return { type: "file", data: file, svg: '<rect class="node-box file" x="' + position.x + '" y="' + position.y + '" width="' + position.w + '" height="' + position.h + '"></rect>' + svgText(position.x + position.w / 2, position.y + 23, file.name, "node-name", "middle") + svgText(position.x + position.w / 2, position.y + 42, file.folder || "根目录", "node-path", "middle") };
}

function svgText(x, y, text, className, anchor) {
  return '<text class="' + className + '" x="' + x + '" y="' + y + '" text-anchor="' + anchor + '">' + escapeHtml(text) + "</text>";
}

function svgLine(x1, y1, x2, y2, className, arrow) {
  return '<path class="' + className + '" d="M ' + x1 + " " + y1 + " L " + x2 + " " + y2 + '"' + (arrow ? ' marker-end="url(#arrow)"' : "") + "></path>";
}

function showFolderDetails(folder) {
  const details = document.getElementById("details");
  if (!details || !folder) return;
  details.innerHTML = "<h2>" + escapeHtml(folder.path || "PayDay") + "</h2><p>" + escapeHtml(state.folderMeanings[folder.path] || "PayDay Java 源码目录") + "</p><p>当前层级包含 " + folder.files.length + " 个直接文件、" + folder.folders.size + " 个直接子目录，共 " + folderCount(folder) + " 个文件。</p>";
}

function showFileDetails(file) {
  const details = document.getElementById("details");
  if (!details || !file) return;
  const links = (file.links || []).filter(path => state.fileByPath.has(path)).slice(0, 18);
  details.innerHTML = "<h2>" + escapeHtml(file.name) + "</h2><p><code>" + escapeHtml(file.path) + "</code></p><p>" + escapeHtml(file.doc) + "</p>" + (links.length ? '<div class="detail-links">' + links.map(path => '<button class="detail-link" data-detail-file="' + escapeAttr(path) + '">' + escapeHtml(path) + "</button>").join("") + "</div>" : "");
  details.querySelectorAll("[data-detail-file]").forEach(button => button.addEventListener("click", () => showFileDetails(state.fileByPath.get(button.dataset.detailFile))));
}

function showSystemDetails(system) {
  const details = document.getElementById("details");
  if (details) details.innerHTML = "<h2>" + escapeHtml(system.title) + "</h2><p>" + escapeHtml(system.description) + "</p><p>本系统包含 " + system.count + " 个文件；矩形框表示按目录分组，金色箭头表示源码中的内部引用。</p>";
}

function showLoadError(error) {
  const target = document.getElementById("details") || document.body;
  target.innerHTML = '<div class="notice">数据加载失败：' + escapeHtml(error.message || error) + "。请通过本地静态服务器打开此目录，而不是直接双击 HTML 文件。</div>";
}

function sortByName(a, b) { return (a.name || a.path).localeCompare(b.name || b.path, "zh-CN"); }
function escapeHtml(value) { return String(value ?? "").replace(/[&<>"']/g, char => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", "\"": "&quot;", "'": "&#39;" }[char])); }
function escapeAttr(value) { return escapeHtml(value); }

