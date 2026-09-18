package fit.tedu.HeThong.service;

import fit.tedu.HeThong.dto.response.ArchiveFileResponse;
import fit.tedu.HeThong.entity.ArchiveFile;
import fit.tedu.HeThong.repository.ArchiveFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

@Service @RequiredArgsConstructor
public class ArchiveService {
    private final ArchiveFileRepository repository;
    @Value("${app.archive.dir:./data/archive}")
    private String directory;

    public List<ArchiveFileResponse> list(String username) {
        return repository.findByUploadedByOrderByCreatedAtDesc(username).stream().map(this::response).collect(Collectors.toList());
    }
    public ArchiveFileResponse upload(MultipartFile file, String username) {
        if (file == null || file.isEmpty()) throw new RuntimeException("Vui lòng chọn file");
        try {
            Path dir = Paths.get(directory).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            String stored = UUID.randomUUID() + "_" + sanitize(file.getOriginalFilename());
            Files.copy(file.getInputStream(), dir.resolve(stored), StandardCopyOption.REPLACE_EXISTING);
            return response(repository.save(ArchiveFile.builder().originalName(file.getOriginalFilename())
                    .storedName(stored).contentType(file.getContentType() == null ? "application/octet-stream" : file.getContentType())
                    .size(file.getSize()).uploadedBy(username).build()));
        } catch (Exception e) { throw new RuntimeException("Không thể lưu file", e); }
    }
    public ArchiveFileResponse saveGenerated(String fileName, byte[] content, String contentType, String username) {
        try {
            Path dir = Paths.get(directory).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            String stored = UUID.randomUUID() + "_" + sanitize(fileName);
            Path target = dir.resolve(stored);
            Files.write(target, content, StandardOpenOption.CREATE_NEW);
            return response(repository.save(ArchiveFile.builder().originalName(fileName).storedName(stored)
                    .contentType(contentType).size(content.length).uploadedBy(username)
                    .createdAt(LocalDateTime.now()).build()));
        } catch (Exception e) {
            throw new RuntimeException("Không thể lưu file vào lưu trữ", e);
        }
    }
    public Resource download(Long id) {
        try {
            ArchiveFile file = repository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy file"));
            Resource resource = new UrlResource(Paths.get(directory).toAbsolutePath().resolve(file.getStoredName()).toUri());
            if (!resource.exists()) throw new RuntimeException("File lưu trữ không còn tồn tại");
            return resource;
        } catch (Exception e) { throw new RuntimeException(e.getMessage(), e); }
    }
    public ArchiveFile get(Long id) { return repository.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy file")); }
    private String sanitize(String name) { return name == null ? "file" : name.replaceAll("[^a-zA-Z0-9._-]", "_"); }
    private ArchiveFileResponse response(ArchiveFile f) { return ArchiveFileResponse.builder().id(f.getId()).originalName(f.getOriginalName()).contentType(f.getContentType()).size(f.getSize()).uploadedBy(f.getUploadedBy()).createdAt(f.getCreatedAt()).build(); }
}
