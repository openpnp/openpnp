{ lib
, fetchFromGitHub
, jdk17
, makeBinaryWrapper
, wrapGAppsHook
, makeDesktopItem
, maven
}:

let
  # OpenPnP depends on a range of version, select the latest.
  jdk = jdk17;
in

# LumenPnP Feeders require OpenPNP version 2023.04.05. However there does not seems to exist any new
# stable release of OpenPNP since 2023.03.15, thus we use the latest tested version.
maven.buildMavenPackage rec {
  pname = "OpenPnP";
  version = "2023-01-28.nbp.photon-vision";
  src = ./.;

  mvnHash = "sha256-paC6l5u9YCfeVE6Q4Rf3LRsRwTvMytRAnSw957iNSS0=";

  nativeBuildInputs = [ maven jdk makeBinaryWrapper wrapGAppsHook ];

  # OpenPnP seems to depend on some software which requires a license for building the installer.
  # To avoid this dependency, we just install everything from the build output.
  installPhase = ''
    mkdir -p $out/bin $out/share/openpnp
    # Replicate what is being done by the install4j installer.
    cp -r target/lib $out/share/openpnp/lib
    cp target/openpnp*.jar $out/share/openpnp
    for f in LICENSE.txt CHANGES.md OPENPNP*.md samples; do
      cp -r $f $out/share/openpnp/.;
    done
    cat > $out/share/openpnp/OpenPnP <<EOF
#!/bin/sh
java \$1 \\
    --add-opens=java.base/java.lang=ALL-UNNAMED \\
    --add-opens=java.desktop/java.awt=ALL-UNNAMED \\
    --add-opens=java.desktop/java.awt.color=ALL-UNNAMED \\
    -jar $out/share/openpnp/openpnp-gui-0.0.1-alpha-SNAPSHOT.jar
EOF
    chmod u+x $out/share/openpnp/OpenPnP
  '';

  dontWrapGApps = true;
  preFixup = ''
    makeBinaryWrapper $out/share/openpnp/OpenPnP $out/bin/OpenPnP \
      --prefix PATH : ${lib.makeBinPath  [ jdk ]} \
      "''${gappsWrapperArgs[@]-}"
  '';

  desktopItems = lib.toList (makeDesktopItem {
    name = "OpenPnP";
    desktopName = "Open Pick and Place";
    exec = "OpenPnP";
    icon = "OpenPnP";
    categories = [ "Development" ];
  });

  meta = {
    homepage = "https://openpnp.org/";
    description = ''
      OpenPnP is an Open Source SMT pick and place system that includes ready to
      run software, and hardware designs that you can build and modify. You can
      also use OpenPnP software to run a pick and place machine of your own
      design, or with existing commercial machines, giving them abilities they
      never had with their OEM software.
    '';
    # Maintainer of the OpenPnP fork at github.com/nbp/OpenPnP.
    maintainers = [ lib.maintainers.nbp ];
    license = lib.licenses.gpl3;
  };
}
