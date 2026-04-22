import { useState } from "react";
import { generateImage } from "./lib/imageApi";

const modelOptions = [
  { value: "kling-v3", label: "kling-v3" },
  { value: "kling-v3-omni", label: "kling-v3-omni" }
];

const modeOptions = [
  { value: "text", label: "文本生图" },
  { value: "reference", label: "图片参考" }
];

const aspectRatios = ["16:9", "9:16", "1:1", "4:3", "3:4", "3:2", "2:3", "21:9"];
const clarityOptions = ["1K", "2K"];
const quantityOptions = ["1", "2", "3", "4", "5", "6", "7", "8", "9"];
const libraryTabs = ["全部", "图片", "视频", "音频"];

const starterPrompts = [
  "A cute chubby cat wearing a blue scarf, cinematic lighting",
  "Futuristic studio product shot, glossy materials, teal accent light",
  "A quiet tea room in rainy Tokyo, soft film look"
];

function App() {
  const [model, setModel] = useState("kling-v3");
  const [mode, setMode] = useState("text");
  const [negativePrompt, setNegativePrompt] = useState("");
  const [prompt, setPrompt] = useState(starterPrompts[0]);
  const [aspectRatio, setAspectRatio] = useState("1:1");
  const [clarity, setClarity] = useState("1K");
  const [quantity, setQuantity] = useState("1");
  const [activeTab, setActiveTab] = useState("全部");
  const [isGenerating, setIsGenerating] = useState(false);
  const [error, setError] = useState("");
  const [result, setResult] = useState(null);

  async function handleGenerate() {
    if (!prompt.trim()) {
      setError("请输入图片描述。");
      return;
    }

    setIsGenerating(true);
    setError("");

    try {
      const payload = {
        model,
        mode,
        prompt: prompt.trim(),
        negativePrompt: negativePrompt.trim(),
        aspectRatio,
        clarity,
        quantity: Number(quantity)
      };

      const response = await generateImage(payload);
      setResult({
        ...response,
        prompt: payload.prompt,
        model: payload.model,
        createdAt: new Date().toLocaleString("zh-CN")
      });
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "生成失败，请稍后再试。");
    } finally {
      setIsGenerating(false);
    }
  }

  return (
    <div className="app-shell">
      <header className="topbar">
        <div className="brand-area">
          <button className="icon-button" type="button" aria-label="Open navigation">
            <span />
            <span />
            <span />
          </button>
          <button className="icon-button plus" type="button" aria-label="Create project">
            +
          </button>
          <div>
            <div className="brand-title">内部创作工具</div>
            <div className="brand-subtitle">React 图片生成面板</div>
          </div>
        </div>
        <button className="settings-button" type="button">
          API 设置
        </button>
      </header>

      <main className="workspace">
        <div className="channel-switch">
          <button className="ghost-tab" type="button">
            视频创作
          </button>
          <button className="primary-tab" type="button">
            图片创作
          </button>
        </div>

        <section className="grid-layout">
          <aside className="panel left-panel">
            <ConfigGroup label="模型">
              <SegmentedControl options={modelOptions} value={model} onChange={setModel} />
            </ConfigGroup>

            <ConfigGroup label="生成模式">
              <SegmentedControl options={modeOptions} value={mode} onChange={setMode} />
            </ConfigGroup>

            <ConfigGroup label="负向提示词（可选）">
              <textarea
                className="input textarea"
                value={negativePrompt}
                onChange={(event) => setNegativePrompt(event.target.value)}
                placeholder="描述不希望出现的内容"
              />
            </ConfigGroup>

            <ConfigGroup label="宽高比">
              <div className="pill-grid">
                {aspectRatios.map((item) => (
                  <button
                    key={item}
                    className={`pill ${aspectRatio === item ? "active" : ""}`}
                    type="button"
                    onClick={() => setAspectRatio(item)}
                  >
                    {item}
                  </button>
                ))}
              </div>
            </ConfigGroup>

            <div className="dual-group">
              <ConfigGroup label="清晰度">
                <div className="tiny-grid two-col">
                  {clarityOptions.map((item) => (
                    <button
                      key={item}
                      className={`tile ${clarity === item ? "active" : ""}`}
                      type="button"
                      onClick={() => setClarity(item)}
                    >
                      {item}
                    </button>
                  ))}
                </div>
              </ConfigGroup>

              <ConfigGroup label="生成数量">
                <div className="tiny-grid three-col">
                  {quantityOptions.map((item) => (
                    <button
                      key={item}
                      className={`tile ${quantity === item ? "active" : ""}`}
                      type="button"
                      onClick={() => setQuantity(item)}
                    >
                      {item}
                    </button>
                  ))}
                </div>
              </ConfigGroup>
            </div>
          </aside>

          <section className="center-column">
            <div className="panel prompt-panel">
              <div className="prompt-input" contentEditable={false}>
                <textarea
                  className="prompt-textarea"
                  value={prompt}
                  onChange={(event) => setPrompt(event.target.value)}
                  placeholder="请输入你想生成的图片描述"
                />
              </div>

              <div className="prompt-footer">
                <div className="starter-links">
                  {starterPrompts.map((item) => (
                    <button key={item} type="button" className="starter-link" onClick={() => setPrompt(item)}>
                      {item}
                    </button>
                  ))}
                </div>
                <button className="generate-button" type="button" onClick={handleGenerate} disabled={isGenerating}>
                  {isGenerating ? "生成中..." : "开始生成"}
                </button>
              </div>
            </div>

            <div className="panel result-panel">
              {error ? <div className="error-banner">{error}</div> : null}

              {result ? (
                <div className="result-content">
                  <img className="result-image" src={result.imageUrl} alt={result.prompt} />
                  <div className="result-meta">
                    <div>
                      <div className="meta-label">返回 URL</div>
                      <a className="result-url" href={result.imageUrl} target="_blank" rel="noreferrer">
                        {result.imageUrl}
                      </a>
                    </div>
                    <div className="meta-grid">
                      <MetaItem label="模型" value={result.model} />
                      <MetaItem label="时间" value={result.createdAt} />
                      <MetaItem label="请求 ID" value={result.requestId} />
                    </div>
                  </div>
                </div>
              ) : (
                <div className="empty-state">
                  <div className="empty-icon">🖼</div>
                  <div className="empty-title">配置参数后点击“开始生成”</div>
                  <div className="empty-text">生成完成后，这里会展示图片和最终返回的 URL。</div>
                </div>
              )}
            </div>
          </section>

          <aside className="panel right-panel">
            <div className="library-title">通用素材库</div>
            <div className="library-tabs">
              {libraryTabs.map((item) => (
                <button
                  key={item}
                  className={`library-tab ${activeTab === item ? "active" : ""}`}
                  type="button"
                  onClick={() => setActiveTab(item)}
                >
                  {item}
                </button>
              ))}
            </div>

            <div className="upload-row">
              <button className="upload-button" type="button">
                + 上传素材
              </button>
              <button className="refresh-button" type="button" aria-label="Refresh assets">
                ↻
              </button>
            </div>

            <div className="asset-empty">暂无素材，点击上方按钮上传</div>
          </aside>
        </section>
      </main>
    </div>
  );
}

function ConfigGroup({ label, children }) {
  return (
    <section className="config-group">
      <div className="group-label">{label}</div>
      {children}
    </section>
  );
}

function SegmentedControl({ options, value, onChange }) {
  return (
    <div className="segmented-control">
      {options.map((option) => (
        <button
          key={option.value}
          className={`segment ${value === option.value ? "active" : ""}`}
          type="button"
          onClick={() => onChange(option.value)}
        >
          {option.label}
        </button>
      ))}
    </div>
  );
}

function MetaItem({ label, value }) {
  return (
    <div className="meta-item">
      <div className="meta-label">{label}</div>
      <div className="meta-value">{value}</div>
    </div>
  );
}

export default App;
