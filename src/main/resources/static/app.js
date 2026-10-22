// Tela simples em JavaScript puro, só para demonstrar a API.
// O token fica no sessionStorage (some ao fechar a aba).

const $ = (id) => document.getElementById(id);
let sessao = null;
let atualizacao = null;

function lerSessao() {
    try { return JSON.parse(sessionStorage.getItem("sessao")); } catch { return null; }
}

function guardarSessao(dados) {
    sessao = dados;
    try {
        if (dados) sessionStorage.setItem("sessao", JSON.stringify(dados));
        else sessionStorage.removeItem("sessao");
    } catch { /* navegação privada: segue só em memória */ }
}

async function api(caminho, opcoes = {}) {
    const cabecalhos = opcoes.headers || {};
    if (sessao) cabecalhos["Authorization"] = "Bearer " + sessao.token;
    const resposta = await fetch(caminho, { ...opcoes, headers: cabecalhos });
    if (resposta.status === 401 && sessao) { sair(); throw new Error("Sessão expirada. Entre de novo."); }
    if (resposta.status === 204) return null;
    const corpo = await resposta.json().catch(() => null);
    if (!resposta.ok) throw new Error((corpo && (corpo.detail || corpo.title)) || "Erro " + resposta.status);
    return corpo;
}

function escapar(texto) {
    const div = document.createElement("div");
    div.textContent = texto ?? "";
    return div.innerHTML;
}

// ---------- entrada ----------

async function autenticar(caminho, formulario) {
    $("erro-entrada").textContent = "";
    const dados = Object.fromEntries(new FormData(formulario));
    try {
        const resposta = await api(caminho, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(dados),
        });
        guardarSessao(resposta);
        mostrarApp();
    } catch (e) {
        $("erro-entrada").textContent = e.message;
    }
}

$("form-entrar").addEventListener("submit", (e) => { e.preventDefault(); autenticar("/api/auth/entrar", e.target); });
$("form-registrar").addEventListener("submit", (e) => { e.preventDefault(); autenticar("/api/auth/registrar", e.target); });
$("sair").addEventListener("click", sair);

function sair() {
    guardarSessao(null);
    clearInterval(atualizacao);
    $("tela-app").hidden = true;
    $("sessao").hidden = true;
    $("tela-entrada").hidden = false;
}

function mostrarApp() {
    $("tela-entrada").hidden = true;
    $("tela-app").hidden = false;
    $("sessao").hidden = false;
    $("quem").textContent = sessao.nome + (sessao.papel === "ADMIN" ? " (admin)" : "");
    $("form-envio").hidden = sessao.papel !== "ADMIN";
    carregarDocumentos();
    clearInterval(atualizacao);
    atualizacao = setInterval(carregarDocumentos, 3000);
}

// ---------- perguntas ----------

const ROTULOS = {
    COMPLETA: "Resposta gerada pela IA",
    SOMENTE_TRECHOS: "Só os trechos (modelo indisponível ou resposta rejeitada)",
    SEM_CONTEXTO: "Sem informação nos documentos",
};

$("form-pergunta").addEventListener("submit", async (e) => {
    e.preventDefault();
    const botao = e.target.querySelector("button");
    $("erro-pergunta").textContent = "";
    botao.disabled = true;
    try {
        const pergunta = new FormData(e.target).get("pergunta");
        const r = await api("/api/perguntas", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ pergunta }),
        });
        mostrarResposta(r);
    } catch (erro) {
        $("erro-pergunta").textContent = erro.message;
    } finally {
        botao.disabled = false;
    }
});

function mostrarResposta(r) {
    $("resposta").hidden = false;
    $("resposta-cabecalho").innerHTML =
        `<span class="etiqueta ${r.modo}">${ROTULOS[r.modo] || r.modo}</span>` +
        (r.doCache ? ` <span class="etiqueta SEM_CONTEXTO">do cache</span>` : "");

    // transforma [1], [2]... em links para a fonte correspondente
    $("resposta-texto").innerHTML = escapar(r.resposta)
        .replace(/\[(?:trecho\s*)?(\d{1,3})\]/gi, '<a class="citacao" href="#fonte-$1">[$1]</a>');

    $("fontes").innerHTML = r.fontes.map((f) => `
        <li id="fonte-${f.numero}" class="${f.citada ? "citada" : ""}">
            <strong>${escapar(f.documento)}</strong>
            <span class="meta">trecho ${f.trecho} · similaridade ${f.similaridade.toFixed(2)}</span>
            <blockquote>${escapar(f.conteudo)}</blockquote>
        </li>`).join("");
}

// ---------- documentos ----------

async function carregarDocumentos() {
    try {
        const docs = await api("/api/documentos");
        $("lista-documentos").innerHTML = docs.map((d) => `
            <tr>
                <td>${escapar(d.titulo)}</td>
                <td class="${d.status}" title="${escapar(d.mensagemErro || "")}">${d.status}</td>
                <td>${d.quantidadeTrechos ?? "-"}</td>
                <td>${sessao.papel === "ADMIN" ? acoes(d) : ""}</td>
            </tr>`).join("") || `<tr><td colspan="4">Nenhum documento ainda.</td></tr>`;
    } catch { /* tenta de novo na próxima atualização */ }
}

function acoes(d) {
    const reprocessar = d.status === "FALHOU"
        ? `<button class="secundario" onclick="reprocessar('${d.id}')">Reprocessar</button> ` : "";
    return reprocessar + `<button class="secundario" onclick="remover('${d.id}')">Remover</button>`;
}

async function reprocessar(id) {
    await api(`/api/documentos/${id}/reprocessar`, { method: "POST" }).catch((e) => alert(e.message));
    carregarDocumentos();
}

async function remover(id) {
    if (!confirm("Remover este documento e todos os trechos dele?")) return;
    await api(`/api/documentos/${id}`, { method: "DELETE" }).catch((e) => alert(e.message));
    carregarDocumentos();
}

$("form-envio").addEventListener("submit", async (e) => {
    e.preventDefault();
    const botao = e.target.querySelector("button");
    $("erro-envio").textContent = "";
    botao.disabled = true;
    try {
        await api("/api/documentos", { method: "POST", body: new FormData(e.target) });
        e.target.reset();
        carregarDocumentos();
    } catch (erro) {
        $("erro-envio").textContent = erro.message;
    } finally {
        botao.disabled = false;
    }
});

// ---------- início ----------

sessao = lerSessao();
if (sessao) mostrarApp();
