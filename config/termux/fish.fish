# İsteğe bağlı Fish oturumunda mevcut Claude yönlendirmesini koru.
if status is-interactive
    function claude --description 'Claude: mevcut Ubuntu proot ortamı'
        proot-distro login ubuntu --bind "$PWD:$PWD" --work-dir "$PWD" -- claude $argv
    end

    # Özel sembol yazı tipi gerektirmeyen kısa, telefona uygun istem.
    set -gx STARSHIP_CONFIG /data/data/com.termux/files/home/.config/termux-panel/starship.toml
    if type -q starship
        starship init fish | source
    end
end
