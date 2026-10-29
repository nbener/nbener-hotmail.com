# Ücretsiz kotalı yapay zekâ seçenekleri

Kontrol tarihi: 29 Ekim 2026. Ücretsiz kota sınırsız kullanım değildir; hesap, bölge ve model koşulları değişebilir. Telefonda gerçek hız veya Türkçe kalite karşılaştırması henüz yapılmadı.

| Hizmet | Kullanım biçimi | Sezai durumu |
| --- | --- | --- |
| Groq | Ücretsiz plan ve modele göre istek/token sınırları | GPT-OSS 120B bağlantısı eklendi |
| Gemini | Bazı modellerde ücretsiz katman; ücretsiz içerikler ürün geliştirmede kullanılabilir | Gemini Flash bağlantısı eklendi |
| OpenRouter | Ücretsiz modeller ve günlük kota | Yalnızca openrouter/free; ücretli modele otomatik geçiş yok |
| ModelScope | Ücretsiz API olanakları var; bölge ve model koşulları ayrıca kontrol edilmeli | Henüz eklenmedi |
| Hugging Face | Inference Providers ücretsiz hesapta aylık 0,10 dolar kredi; model dosyası indirmek ayrı konu | Henüz eklenmedi |
| Cloudflare Workers AI | Günlük 10.000 neuron ücretsiz kullanım; istek sayısı değildir | Henüz eklenmedi |
| Cerebras | Güncel başlangıç kredisi süreli; kalıcı ücretsiz plan sayılmamalı | Henüz eklenmedi |

Resmî kaynaklar:

- Groq modelleri ve sınırları: https://console.groq.com/docs/models ve https://console.groq.com/docs/rate-limits
- Groq API biçimi: https://console.groq.com/docs/openai ve https://console.groq.com/docs/reasoning
- Gemini fiyatlandırma ve veri koşulları: https://ai.google.dev/gemini-api/docs/pricing
- Gemini uyumluluk: https://ai.google.dev/gemini-api/docs/openai
- OpenRouter ücretsiz modeller: https://openrouter.ai/collections/free-models
- ModelScope: https://modelscope.ai/posts/434362
- Hugging Face: https://huggingface.co/docs/inference-providers/pricing
- Cloudflare: https://developers.cloudflare.com/workers-ai/platform/pricing/
- Cerebras: https://www.cerebras.ai/pricing

Her sağlayıcının kendi API anahtarı gerekir. Anahtar yalnızca uygulamanın bağlantı ekranına girilir; sohbetten istenmez. Anahtarlar ayrı Android Keystore kasalarında şifreli tutulur. Canlı API ve sesli uçtan uca doğrulama, gerçek anahtar girilmediği için henüz yapılmadı. Otomatik sağlayıcı değiştirme yoktur. Ücretli hesaba bağlı anahtarların kullanımında sağlayıcının hesap koşulları geçerlidir.
